package z.caAuth.confirmation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import z.caAuth.account.AccountManager;
import z.caAuth.bots.Discord.DiscordBot;
import z.caAuth.bots.Telegram.TelegramBot;
import z.caAuth.config.MessagesConfig;
import z.caAuth.config.AuthConfig;
import z.caAuth.util.ColorManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class LoginConfirmationManager {
    private final JavaPlugin plugin;
    private final AccountManager accountManager;
    private final AuthConfig pluginConfig;
    private final MessagesConfig messagesConfig;

    private volatile DiscordBot discordBot;
    private volatile TelegramBot telegramBot;

    private final Map<String, Pending> pendingByToken = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeTokenByPlayer = new ConcurrentHashMap<>();

    public LoginConfirmationManager(JavaPlugin plugin, AccountManager accountManager,
                                     AuthConfig pluginConfig, MessagesConfig messagesConfig) {
        this.plugin = plugin;
        this.accountManager = accountManager;
        this.pluginConfig = pluginConfig;
        this.messagesConfig = messagesConfig;
    }

    public void setDiscordBot(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    public void setTelegramBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void requestConfirmation(Player player) {
        if (!pluginConfig.isLoginConfirmationEnabled()) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        if (!accountManager.isAccountBound(playerUUID)) {
            return;
        }

        long now = System.currentTimeMillis() / 1000L;
        long lastConfirmed = accountManager.getLastConfirmedAt(playerUUID);
        long intervalSeconds = Math.max(0, pluginConfig.getLoginConfirmationIntervalHours()) * 3600L;
        if (intervalSeconds > 0 && now - lastConfirmed < intervalSeconds) {
            return;
        }

        cancelPending(playerUUID);

        String bindingType = accountManager.getBindingType(playerUUID);
        String bindingId = accountManager.getBindingId(playerUUID);
        int timeoutSeconds = Math.max(5, pluginConfig.getLoginConfirmationTimeoutSeconds());
        String token = UUID.randomUUID().toString();
        String playerName = player.getName();

        boolean sent = false;
        if ("discord".equals(bindingType) && discordBot != null && discordBot.isRunning()) {
            sent = discordBot.sendLoginConfirmation(bindingId, playerName, token, timeoutSeconds);
            if (sent) {
                ColorManager.sendMessage(player, messagesConfig.getLoginConfirmationSentDiscord(timeoutSeconds));
            }
        } else if ("telegram".equals(bindingType) && telegramBot != null && telegramBot.isRunning()) {
            sent = telegramBot.sendLoginConfirmation(bindingId, playerName, token, timeoutSeconds);
            if (sent) {
                ColorManager.sendMessage(player, messagesConfig.getLoginConfirmationSentTelegram(timeoutSeconds));
            }
        }

        if (!sent) {
            plugin.getLogger().warning("Не удалось отправить запрос подтверждения входа игроку "
                    + playerName + " (бот " + bindingType + " недоступен). Игрок будет кикнут.");
            player.kickPlayer(messagesConfig.getLoginConfirmationBotUnavailableKick());
            return;
        }

        BukkitTask kickTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingByToken.remove(token);
            activeTokenByPlayer.remove(playerUUID, token);
            Player online = Bukkit.getPlayer(playerUUID);
            if (online != null && online.isOnline()) {
                online.kickPlayer(messagesConfig.getLoginConfirmationTimeoutKick());
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
                ColorManager.sendMessage(player, messagesConfig.getLoginConfirmationConfirmed());
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
                player.kickPlayer(messagesConfig.getLoginConfirmationDeniedKick());
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
