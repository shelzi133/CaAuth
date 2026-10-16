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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        return postAll("/kick", body, "кик")
                .thenApply(responses -> responses.stream()
                        .anyMatch(response -> response.get("kicked").getAsBoolean()));
    }

    public CompletableFuture<PasswordResult> changePassword(String name, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("password", password);
        return postAll("/password/change", body, "смена пароля")
                .thenApply(responses -> {
                    List<String> results = responses.stream()
                            .map(response -> response.get("result").getAsString())
                            .toList();
                    if (results.contains("OK")) {
                        return PasswordResult.OK;
                    }
                    return results.contains("NOT_FOUND") ? PasswordResult.NOT_FOUND : PasswordResult.UNAVAILABLE;
                });
    }

    public CompletableFuture<PlayerLookup> findPlayer(String name) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        return postAll("/player/exists", body, "проверка игрока")
                .thenApply(responses -> {
                    if (responses.isEmpty()) {
                        return new PlayerLookup(false, false, null);
                    }
                    for (JsonObject response : responses) {
                        if (response.get("exists").getAsBoolean()) {
                            return new PlayerLookup(true, true, realName(response, name));
                        }
                    }
                    return new PlayerLookup(true, false, realName(responses.get(0), name));
                });
    }

    private String realName(JsonObject response, String fallback) {
        return response.has("realName") && !response.get("realName").isJsonNull()
                ? response.get("realName").getAsString()
                : fallback;
    }

    private List<MainConfig.ProxyApi> endpoints() {
        MainConfig.Velocity velocity = mainConfig.module.auth.velocity;
        List<MainConfig.ProxyApi> endpoints = new ArrayList<>();
        endpoints.add(velocity.proxyApi);
        if (velocity.additionalProxyApis != null) {
            endpoints.addAll(velocity.additionalProxyApis);
        }
        return endpoints;
    }

    private CompletableFuture<List<JsonObject>> postAll(String path, JsonObject body, String label) {
        List<CompletableFuture<JsonObject>> futures = new ArrayList<>();
        for (MainConfig.ProxyApi api : endpoints()) {
            futures.add(post(api, path, body).exceptionally(error -> {
                logger.warning("Velocity " + api.host + ":" + api.port + " (" + label + "): " + describe(error));
                return null;
            }));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList());
    }

    private CompletableFuture<JsonObject> post(MainConfig.ProxyApi api, String path, JsonObject body) {
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
