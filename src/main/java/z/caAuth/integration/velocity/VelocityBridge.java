package z.caAuth.integration.velocity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.account.AccountManager;
import z.caAuth.account.BindingType;
import z.caAuth.binding.TwinkService;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.MainConfig;
import z.caAuth.login.LoginConfirmationManager;
import z.caAuth.login.LoginConfirmationManager.ExternalResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VelocityBridge {

    private static final int MAX_BODY_BYTES = 4096;
    private static final String SECRET_HEADER = "X-Auth-Secret";

    private final JavaPlugin plugin;
    private final MainConfig mainConfig;
    private final AuthConfig authConfig;
    private final AccountManager accountManager;
    private final LoginConfirmationManager confirmationManager;
    private final TwinkService twinkService;

    private HttpServer httpServer;
    private ExecutorService executor;
    private byte[] secret;

    public VelocityBridge(JavaPlugin plugin, MainConfig mainConfig, AuthConfig authConfig,
                          AccountManager accountManager,
                          LoginConfirmationManager confirmationManager, TwinkService twinkService) {
        this.plugin = plugin;
        this.mainConfig = mainConfig;
        this.authConfig = authConfig;
        this.accountManager = accountManager;
        this.confirmationManager = confirmationManager;
        this.twinkService = twinkService;
    }

    @FunctionalInterface
    private interface Endpoint {
        JsonObject handle(JsonObject request) throws Exception;
    }

    public boolean start() {
        if (!mainConfig.isVelocityMode()) {
            return false;
        }

        MainConfig.Velocity settings = mainConfig.module.auth.velocity;
        if (settings.secret == null || settings.secret.isBlank()) {
            plugin.getLogger().warning("module.auth.velocity.secret не задан, API для Velocity не запущено");
            return false;
        }

        secret = settings.secret.getBytes(StandardCharsets.UTF_8);

        try {
            executor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "CaAuth-Velocity-API");
                thread.setDaemon(true);
                return thread;
            });
            httpServer = HttpServer.create(new InetSocketAddress(settings.host, settings.port), 0);
            httpServer.createContext("/check", endpoint(this::check));
            httpServer.createContext("/bind/code", endpoint(this::bindCode));
            httpServer.createContext("/bind/status", endpoint(this::bindStatus));
            httpServer.createContext("/confirm", endpoint(this::confirm));
            httpServer.createContext("/cancel", endpoint(this::cancel));
            httpServer.createContext("/twink/redeem", endpoint(this::redeemTwink));
            httpServer.setExecutor(executor);
            httpServer.start();
            plugin.getLogger().info("API для Velocity запущено на " + settings.host + ":" + settings.port);
            return true;
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().severe("Не удалось запустить API для Velocity: " + e.getMessage());
            stop();
            return false;
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private JsonObject check(JsonObject request) {
        UUID uuid = uuid(request);
        String ip = string(request, "ip");

        JsonObject response = new JsonObject();
        response.addProperty("timeoutSeconds", confirmationManager.getTimeoutSeconds());

        if (!accountManager.isAccountBound(uuid)) {
            boolean twinkInvite = twinkService.hasActiveInvite(string(request, "name"));
            response.addProperty("action", twinkInvite ? "TWINK" : "BIND");
            return response;
        }

        BindingType type = accountManager.getBindingType(uuid);
        response.addProperty("type", type != null ? type.getId() : null);
        response.addProperty("action", needsConfirmation(uuid, ip) ? "CONFIRM" : "ALLOW");
        return response;
    }

    private JsonObject redeemTwink(JsonObject request) {
        UUID uuid = uuid(request);
        String ip = string(request, "ip");

        TwinkService.RedeemResult result = twinkService.redeem(uuid, string(request, "name"), string(request, "code"));
        if (result.status() == TwinkService.RedeemStatus.BOUND) {
            stamp(uuid, ip);
        }

        JsonObject response = new JsonObject();
        response.addProperty("result", result.status().name());
        response.addProperty("minutes", result.lockedMinutes());
        return response;
    }

    private JsonObject bindCode(JsonObject request) {
        UUID uuid = uuid(request);

        JsonObject response = new JsonObject();
        if (accountManager.isAccountBound(uuid)) {
            response.addProperty("bound", true);
            return response;
        }

        response.addProperty("bound", false);
        response.addProperty("code", accountManager.generateVerificationCode(uuid, string(request, "name")));
        return response;
    }

    private JsonObject bindStatus(JsonObject request) {
        UUID uuid = uuid(request);
        String ip = string(request, "ip");

        boolean bound = accountManager.isAccountBound(uuid);
        if (bound) {
            stamp(uuid, ip);
        }

        boolean twinkInvite = !bound && request.has("name")
                && twinkService.hasActiveInvite(request.get("name").getAsString());

        JsonObject response = new JsonObject();
        response.addProperty("bound", bound);
        response.addProperty("twink", twinkInvite);
        return response;
    }

    private JsonObject confirm(JsonObject request) throws Exception {
        UUID uuid = uuid(request);
        String name = string(request, "name");
        String ip = string(request, "ip");

        ExternalResult result = confirmationManager.requestExternalConfirmation(uuid, name).get();
        if (result == ExternalResult.CONFIRMED) {
            stamp(uuid, ip);
        }

        JsonObject response = new JsonObject();
        response.addProperty("result", result.name());
        return response;
    }

    private JsonObject cancel(JsonObject request) {
        confirmationManager.cancelExternal(uuid(request));

        JsonObject response = new JsonObject();
        response.addProperty("ok", true);
        return response;
    }

    private boolean needsConfirmation(UUID uuid, String ip) {
        if (!ip.equals(accountManager.getLastConfirmedIp(uuid))) {
            return true;
        }

        long intervalSeconds = Math.max(0, authConfig.bots.loginConfirmation.intervalHours) * 3600L;
        if (intervalSeconds == 0) {
            return true;
        }

        long elapsed = System.currentTimeMillis() / 1000L - accountManager.getLastConfirmedAt(uuid);
        return elapsed >= intervalSeconds;
    }

    private void stamp(UUID uuid, String ip) {
        accountManager.markConfirmed(uuid, ip);
    }

    private HttpHandler endpoint(Endpoint endpoint) {
        return exchange -> {
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    respond(exchange, 405, error("method not allowed"));
                    return;
                }
                if (!authorized(exchange)) {
                    respond(exchange, 401, error("unauthorized"));
                    return;
                }

                byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
                if (body.length > MAX_BODY_BYTES) {
                    respond(exchange, 413, error("payload too large"));
                    return;
                }

                JsonObject request;
                try {
                    request = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
                } catch (RuntimeException e) {
                    respond(exchange, 400, error("invalid json"));
                    return;
                }

                try {
                    respond(exchange, 200, endpoint.handle(request));
                } catch (IllegalArgumentException e) {
                    respond(exchange, 400, error(e.getMessage()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    respond(exchange, 503, error("interrupted"));
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка в API для Velocity (" + exchange.getRequestURI().getPath() + "): " + e);
                    respond(exchange, 500, error("internal error"));
                }
            } catch (IOException e) {
                plugin.getLogger().fine("Ошибка ответа API для Velocity: " + e.getMessage());
            } finally {
                exchange.close();
            }
        };
    }

    private boolean authorized(HttpExchange exchange) {
        String provided = exchange.getRequestHeaders().getFirst(SECRET_HEADER);
        return provided != null && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), secret);
    }

    private void respond(HttpExchange exchange, int status, JsonObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private JsonObject error(String message) {
        JsonObject body = new JsonObject();
        body.addProperty("error", message);
        return body;
    }

    private String string(JsonObject request, String key) {
        if (!request.has(key) || !request.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("missing field: " + key);
        }
        String value = request.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("empty field: " + key);
        }
        return value;
    }

    private UUID uuid(JsonObject request) {
        try {
            return UUID.fromString(string(request, "uuid"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid uuid");
        }
    }
}