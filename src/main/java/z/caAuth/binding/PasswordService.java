package z.caAuth.binding;

import z.caAuth.account.BindingType;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.MainConfig;
import z.caAuth.integration.velocity.VelocityClient;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PasswordService {

    public enum Status { OK, NOT_BOUND, NOT_REGISTERED, UNAVAILABLE }

    public record Result(Status status, String playerName, String password) {
    }

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = UPPER + LOWER + DIGITS;
    private static final int MIN_LENGTH = 8;

    private final BindingService bindingService;
    private final VelocityClient velocityClient;
    private final MainConfig mainConfig;
    private final AuthConfig authConfig;
    private final SecureRandom random = new SecureRandom();

    public PasswordService(BindingService bindingService, VelocityClient velocityClient,
                           MainConfig mainConfig, AuthConfig authConfig) {
        this.bindingService = bindingService;
        this.velocityClient = velocityClient;
        this.mainConfig = mainConfig;
        this.authConfig = authConfig;
    }

    public boolean isAvailable() {
        return mainConfig.isVelocityMode();
    }

    public CompletableFuture<Result> changePassword(BindingType type, String bindingId) {
        UUID playerUUID = bindingService.getPlayerByBinding(type, bindingId);
        if (playerUUID == null) {
            return CompletableFuture.completedFuture(new Result(Status.NOT_BOUND, null, null));
        }
        String playerName = bindingService.getPlayerName(playerUUID);
        if (!isAvailable() || playerName == null || playerName.isBlank()) {
            return CompletableFuture.completedFuture(new Result(Status.UNAVAILABLE, playerName, null));
        }

        String password = generate();
        return velocityClient.changePassword(playerName, password).thenApply(result -> switch (result) {
            case OK -> new Result(Status.OK, playerName, password);
            case NOT_FOUND -> new Result(Status.NOT_REGISTERED, playerName, null);
            case UNAVAILABLE -> new Result(Status.UNAVAILABLE, playerName, null);
        });
    }

    private String generate() {
        int length = Math.max(MIN_LENGTH, authConfig.bots.passwordChange.length);
        List<Character> chars = new ArrayList<>(length);
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        while (chars.size() < length) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, random);

        StringBuilder builder = new StringBuilder(length);
        chars.forEach(builder::append);
        return builder.toString();
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
