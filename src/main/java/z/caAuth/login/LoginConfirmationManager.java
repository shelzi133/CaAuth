package z.caAuth.login;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import z.caAuth.account.AccountManager;
import z.caAuth.account.BindingType;
import z.caAuth.bots.ChatBot;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.AuthMessagesConfig;
import z.caAuth.util.Colors;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LoginConfirmationManager {
    private final JavaPlugin plugin;
    private final AccountManager accountManager;
    private final AuthConfig pluginConfig;
    private final AuthMessagesConfig authMessagesConfig;

    private final Map<BindingType, ChatBot> bots = new ConcurrentHashMap<>();

    private final Map<String, Pending> pendingByToken = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeTokenByPlayer = new ConcurrentHashMap<>();
    private final Map<String, ExternalPending> externalByToken = new ConcurrentHashMap<>();
    private final Map<UUID, String> externalTokenByPlayer = new ConcurrentHashMap<>();

    public enum ExternalResult {
        CONFIRMED,
        DENIED,
        TIMEOUT,
        UNAVAILABLE,
        CANCELLED
    }

    public LoginConfirmationManager(JavaPlugin plugin, AccountManager accountManager,
                                    AuthConfig pluginConfig, AuthMessagesConfig authMessagesConfig) {
        this.plugin = plugin;
        this.accountManager = accountManager;
        this.pluginConfig = pluginConfig;
        this.authMessagesConfig = authMessagesConfig;
    }

    public void registerBot(ChatBot bot) {
        bots.put(bot.type(), bot);
    }

    public void requestConfirmation(Player player) {
        if (!pluginConfig.bots.loginConfirmation.enabled) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        if (!accountManager.isAccountBound(playerUUID)) {
            return;
        }

        long now = System.currentTimeMillis() / 1000L;
        long lastConfirmed = accountManager.getLastConfirmedAt(playerUUID);
        long intervalSeconds = Math.max(0, pluginConfig.bots.loginConfirmation.intervalHours) * 3600L;
        if (intervalSeconds > 0 && now - lastConfirmed < intervalSeconds) {
            return;
        }

        cancelPending(playerUUID);

        BindingType bindingType = accountManager.getBindingType(playerUUID);
        int timeoutSeconds = getTimeoutSeconds();
        String token = UUID.randomUUID().toString();
        String playerName = player.getName();

        if (!sendRequest(playerUUID, playerName, token, timeoutSeconds)) {
            plugin.getLogger().warning("Не удалось отправить запрос подтверждения входа игроку "
                    + playerName + " (бот " + bindingType + " недоступен). Игрок будет кикнут.");
            player.kickPlayer(authMessagesConfig.getLoginConfirmationBotUnavailableKick());
            return;
        }
        Colors.sendMessage(player, bindingType == BindingType.DISCORD
                ? authMessagesConfig.getLoginConfirmationSentDiscord(timeoutSeconds)
                : authMessagesConfig.getLoginConfirmationSentTelegram(timeoutSeconds));

        BukkitTask kickTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingByToken.remove(token);
            activeTokenByPlayer.remove(playerUUID, token);
            Player online = Bukkit.getPlayer(playerUUID);
            if (online != null && online.isOnline()) {
                online.kickPlayer(authMessagesConfig.getLoginConfirmationTimeoutKick());
            }
        }, timeoutSeconds * 20L);

        pendingByToken.put(token, new Pending(playerUUID, kickTask));
        activeTokenByPlayer.put(playerUUID, token);
    }

    public CompletableFuture<ExternalResult> requestExternalConfirmation(UUID playerUUID, String playerName) {
        cancelExternal(playerUUID);

        int timeoutSeconds = getTimeoutSeconds();
        String token = UUID.randomUUID().toString();

        CompletableFuture<ExternalResult> future = new CompletableFuture<ExternalResult>()
                .completeOnTimeout(ExternalResult.TIMEOUT, timeoutSeconds, TimeUnit.SECONDS);
        externalByToken.put(token, new ExternalPending(playerUUID, future));
        externalTokenByPlayer.put(playerUUID, token);
        future.whenComplete((result, error) -> {
            externalByToken.remove(token);
            externalTokenByPlayer.remove(playerUUID, token);
        });

        if (!sendRequest(playerUUID, playerName, token, timeoutSeconds)) {
            plugin.getLogger().warning("Не удалось отправить запрос подтверждения входа игроку "
                    + playerName + " (бот " + accountManager.getBindingType(playerUUID) + " недоступен).");
            future.complete(ExternalResult.UNAVAILABLE);
        }

        return future;
    }

    public int getTimeoutSeconds() {
        return Math.max(5, pluginConfig.bots.loginConfirmation.timeoutSeconds);
    }

    private boolean sendRequest(UUID playerUUID, String playerName, String token, int timeoutSeconds) {
        BindingType bindingType = accountManager.getBindingType(playerUUID);
        if (bindingType == null) {
            return false;
        }
        ChatBot bot = bots.get(bindingType);
        return bot != null && bot.isRunning()
                && bot.sendLoginConfirmation(accountManager.getBindingId(playerUUID), playerName, token, timeoutSeconds);
    }

    public void cancelExternal(UUID playerUUID) {
        String token = externalTokenByPlayer.get(playerUUID);
        if (token == null) {
            return;
        }
        ExternalPending pending = externalByToken.get(token);
        if (pending != null) {
            pending.future.complete(ExternalResult.CANCELLED);
        }
    }

    public void confirm(String token) {
        ExternalPending external = externalByToken.get(token);
        if (external != null) {
            external.future.complete(ExternalResult.CONFIRMED);
            return;
        }

        Pending pending = pendingByToken.remove(token);
        if (pending == null) {
            return;
        }
        activeTokenByPlayer.remove(pending.playerUUID, token);

        Bukkit.getScheduler().runTask(plugin, () -> {
            pending.kickTask.cancel();
            accountManager.markConfirmed(pending.playerUUID, null);
            Player player = Bukkit.getPlayer(pending.playerUUID);
            if (player != null && player.isOnline()) {
                Colors.sendMessage(player, authMessagesConfig.getLoginConfirmationConfirmed());
            }
        });
    }

    public void deny(String token) {
        ExternalPending external = externalByToken.get(token);
        if (external != null) {
            external.future.complete(ExternalResult.DENIED);
            return;
        }

        Pending pending = pendingByToken.remove(token);
        if (pending == null) {
            return;
        }
        activeTokenByPlayer.remove(pending.playerUUID, token);

        Bukkit.getScheduler().runTask(plugin, () -> {
            pending.kickTask.cancel();
            Player player = Bukkit.getPlayer(pending.playerUUID);
            if (player != null && player.isOnline()) {
                player.kickPlayer(authMessagesConfig.getLoginConfirmationDeniedKick());
            }
        });
    }

    public boolean isPending(UUID playerUUID) {
        return activeTokenByPlayer.containsKey(playerUUID);
    }

    public void cancelPending(UUID playerUUID) {
        String token = activeTokenByPlayer.remove(playerUUID);
        if (token == null) {
            return;
        }
        Pending pending = pendingByToken.remove(token);
        if (pending != null) {
            pending.kickTask.cancel();
        }
    }

    private static final class ExternalPending {
        final UUID playerUUID;
        final CompletableFuture<ExternalResult> future;

        ExternalPending(UUID playerUUID, CompletableFuture<ExternalResult> future) {
            this.playerUUID = playerUUID;
            this.future = future;
        }
    }

    private static final class Pending {
        final UUID playerUUID;
        final BukkitTask kickTask;

        Pending(UUID playerUUID, BukkitTask kickTask) {
            this.playerUUID = playerUUID;
            this.kickTask = kickTask;
        }
    }
}