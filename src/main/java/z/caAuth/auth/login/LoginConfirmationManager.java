package z.caAuth.auth.login;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import z.caAuth.auth.account.AccountManager;
import z.caAuth.bots.discord.DiscordBot;
import z.caAuth.bots.telegram.TelegramBot;
import z.caAuth.auth.config.AuthMessagesConfig;
import z.caAuth.auth.config.AuthConfig;
import z.caAuth.auth.util.ColorManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class LoginConfirmationManager {
    private final JavaPlugin plugin;
    private final AccountManager accountManager;
    private final AuthConfig pluginConfig;
    private final AuthMessagesConfig authMessagesConfig;

    private volatile DiscordBot discordBot;
    private volatile TelegramBot telegramBot;

    private final Map<String, Pending> pendingByToken = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeTokenByPlayer = new ConcurrentHashMap<>();

    public LoginConfirmationManager(JavaPlugin plugin, AccountManager accountManager,
                                     AuthConfig pluginConfig, AuthMessagesConfig authMessagesConfig) {
        this.plugin = plugin;
        this.accountManager = accountManager;
        this.pluginConfig = pluginConfig;
        this.authMessagesConfig = authMessagesConfig;
    }

    public void setDiscordBot(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    public void setTelegramBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
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

        String bindingType = accountManager.getBindingType(playerUUID);
        String bindingId = accountManager.getBindingId(playerUUID);
        int timeoutSeconds = Math.max(5, pluginConfig.bots.loginConfirmation.timeoutSeconds);
        String token = UUID.randomUUID().toString();
        String playerName = player.getName();

        boolean sent = false;
        if ("discord".equals(bindingType) && discordBot != null && discordBot.isRunning()) {
            sent = discordBot.sendLoginConfirmation(bindingId, playerName, token, timeoutSeconds);
            if (sent) {
                ColorManager.sendMessage(player, authMessagesConfig.getLoginConfirmationSentDiscord(timeoutSeconds));
            }
        } else if ("telegram".equals(bindingType) && telegramBot != null && telegramBot.isRunning()) {
            sent = telegramBot.sendLoginConfirmation(bindingId, playerName, token, timeoutSeconds);
            if (sent) {
                ColorManager.sendMessage(player, authMessagesConfig.getLoginConfirmationSentTelegram(timeoutSeconds));
            }
        }

        if (!sent) {
            plugin.getLogger().warning("Не удалось отправить запрос подтверждения входа игроку "
                    + playerName + " (бот " + bindingType + " недоступен). Игрок будет кикнут.");
            player.kickPlayer(authMessagesConfig.getLoginConfirmationBotUnavailableKick());
            return;
        }

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

    public void confirm(String token) {
        Pending pending = pendingByToken.remove(token);
        if (pending == null) {
            return;
        }
        activeTokenByPlayer.remove(pending.playerUUID, token);

        Bukkit.getScheduler().runTask(plugin, () -> {
            pending.kickTask.cancel();
            accountManager.setLastConfirmedAt(pending.playerUUID, System.currentTimeMillis() / 1000L);
            Player player = Bukkit.getPlayer(pending.playerUUID);
            if (player != null && player.isOnline()) {
                ColorManager.sendMessage(player, authMessagesConfig.getLoginConfirmationConfirmed());
            }
        });
    }


    public void deny(String token) {
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

    private static final class Pending {
        final UUID playerUUID;
        final BukkitTask kickTask;

        Pending(UUID playerUUID, BukkitTask kickTask) {
            this.playerUUID = playerUUID;
            this.kickTask = kickTask;
        }
    }
}
