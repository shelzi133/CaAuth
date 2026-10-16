package z.caAuth.banking;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.config.MainConfig;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BankService {

    public enum Status { OK, UNAVAILABLE, NO_CARD, RECIPIENT_NOT_FOUND, OWN_CARD, INVALID_AMOUNT, INSUFFICIENT, ERROR }

    public record Overview(Status status, String cardId, double balance) {
    }

    public record Quote(Status status, String cardId, String recipientName, int amount, double feePercent,
                        double fee, double total, double balance) {
    }

    private static final int MAX_AMOUNT = 1_000_000;

    private final JavaPlugin plugin;
    private final MainConfig mainConfig;

    public BankService(JavaPlugin plugin, MainConfig mainConfig) {
        this.plugin = plugin;
        this.mainConfig = mainConfig;
    }

    public boolean isAvailable() {
        return mainConfig.module.banking.enabled && plugin.getServer().getPluginManager().isPluginEnabled("CalistoBanking");
    }

    public static int parseAmount(String raw) {
        if (raw == null) {
            return -1;
        }
        String digits = raw.trim().replace(" ", "");
        if (digits.isEmpty() || digits.length() > 7 || !digits.chars().allMatch(Character::isDigit)) {
            return -1;
        }
        int value = Integer.parseInt(digits);
        return value >= 1 && value <= MAX_AMOUNT ? value : -1;
    }

    public static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    public CompletableFuture<Overview> overview(UUID owner) {
        return sync(() -> isAvailable()
                ? BankHook.overview(plugin, owner)
                : new Overview(Status.UNAVAILABLE, null, 0), new Overview(Status.ERROR, null, 0));
    }

    public CompletableFuture<Quote> quote(UUID owner, String recipient, int amount) {
        return sync(() -> isAvailable()
                ? BankHook.quote(plugin, owner, recipient, amount)
                : failed(Status.UNAVAILABLE), failed(Status.ERROR));
    }

    public CompletableFuture<Quote> transfer(UUID owner, String cardId, int amount, String source) {
        return sync(() -> isAvailable()
                ? BankHook.transfer(plugin, owner, cardId, amount, source)
                : failed(Status.UNAVAILABLE), failed(Status.ERROR));
    }

    private Quote failed(Status status) {
        return new Quote(status, null, null, 0, 0, 0, 0, 0);
    }

    private <T> CompletableFuture<T> sync(Supplier<T> task, T fallback) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    future.complete(task.get());
                } catch (Throwable error) {
                    plugin.getLogger().warning("Ошибка банковской операции: " + error.getMessage());
                    future.complete(fallback);
                }
            });
        } catch (Exception error) {
            future.complete(fallback);
        }
        return future;
    }
}
