package z.caAuth.integration.velocity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import z.caAuth.config.MainConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

public class VelocityClient {

    public enum PasswordResult { OK, NOT_FOUND, UNAVAILABLE }

    public record PlayerLookup(boolean available, boolean exists, String realName) {
    }

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final MainConfig mainConfig;
    private final Logger logger;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

    public VelocityClient(MainConfig mainConfig, Logger logger) {
        this.mainConfig = mainConfig;
        this.logger = logger;
    }

    public CompletableFuture<Boolean> kick(UUID uuid, String name, String reason) {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", uuid.toString());
        body.addProperty("name", name == null || name.isBlank() ? "-" : name);
        body.addProperty("reason", reason);
        return post("/kick", body)
                .thenApply(response -> response.get("kicked").getAsBoolean())
                .exceptionally(error -> {
                    logger.warning("Не удалось отправить кик в Velocity: " + describe(error));
                    return false;
                });
    }

    public CompletableFuture<PasswordResult> changePassword(String name, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("password", password);
        return post("/password/change", body)
                .thenApply(response -> switch (response.get("result").getAsString()) {
                    case "OK" -> PasswordResult.OK;
                    case "NOT_FOUND" -> PasswordResult.NOT_FOUND;
                    default -> PasswordResult.UNAVAILABLE;
                })
                .exceptionally(error -> {
                    logger.warning("Не удалось сменить пароль через Velocity: " + describe(error));
                    return PasswordResult.UNAVAILABLE;
                });
    }

    public CompletableFuture<PlayerLookup> findPlayer(String name) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        return post("/player/exists", body)
                .thenApply(response -> new PlayerLookup(true, response.get("exists").getAsBoolean(),
                        response.has("realName") && !response.get("realName").isJsonNull()
                                ? response.get("realName").getAsString()
                                : name))
                .exceptionally(error -> {
                    logger.warning("Не удалось проверить игрока в Velocity: " + describe(error));
                    return new PlayerLookup(false, false, null);
                });
    }

    private CompletableFuture<JsonObject> post(String path, JsonObject body) {
        MainConfig.ProxyApi api = mainConfig.module.auth.velocity.proxyApi;
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + api.host + ":" + api.port + path))
                .timeout(Duration.ofSeconds(Math.max(1, api.timeoutSeconds)))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("X-Auth-Secret", mainConfig.module.auth.velocity.secret)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException("HTTP " + response.statusCode() + " " + path));
                    }
                    return JsonParser.parseString(response.body()).getAsJsonObject();
                });
    }

    private String describe(Throwable error) {
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        return cause.toString();
    }
}
