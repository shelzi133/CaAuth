package z.caAuth.auth.integration;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.auth.account.AccountManager;
import z.caAuth.auth.account.AccountStorage;
import z.caAuth.auth.config.AuthMessagesConfig;
import z.caAuth.auth.util.ColorManager;

import java.util.UUID;

public class BotIntegration {
    private final AccountStorage storage;
    private final AccountManager accountManager;
    private final JavaPlugin plugin;
    private final AuthMessagesConfig authMessagesConfig;

    public BotIntegration(AccountStorage storage, AccountManager accountManager, JavaPlugin plugin, AuthMessagesConfig authMessagesConfig) {
        this.storage = storage;
        this.accountManager = accountManager;
        this.plugin = plugin;
        this.authMessagesConfig = authMessagesConfig;
    }

    public boolean bindDiscordAccount(UUID playerUUID, String discordId) {
        UUID existingPlayer = storage.getPlayerByDiscordId(discordId);
        if (existingPlayer != null && !existingPlayer.equals(playerUUID)) {
            return false;
        }
        
        accountManager.bindDiscord(playerUUID, discordId);
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    ColorManager.sendMessage(player, authMessagesConfig.getAccountBoundSuccessDiscord()));
        }
        
        return true;
    }

    public boolean bindTelegramAccount(UUID playerUUID, String telegramId) {
        UUID existingPlayer = storage.getPlayerByTelegramId(telegramId);
        if (existingPlayer != null && !existingPlayer.equals(playerUUID)) {
            return false;
        }
        
        accountManager.bindTelegram(playerUUID, telegramId);

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    ColorManager.sendMessage(player, authMessagesConfig.getAccountBoundSuccessTelegram()));
        }
        
        return true;
    }
    public String generateVerificationCode(UUID playerUUID) {
        return accountManager.generateVerificationCode(playerUUID);
    }
    public boolean verifyAndBind(UUID playerUUID, String code, String discordId, String telegramId) {
        if (!accountManager.verifyCode(playerUUID, code)) {
            return false;
        }
        
        if (discordId != null && !discordId.isEmpty()) {
            return bindDiscordAccount(playerUUID, discordId);
        } else if (telegramId != null && !telegramId.isEmpty()) {
            return bindTelegramAccount(playerUUID, telegramId);
        }
        
        return false;
    }

    public UUID getPlayerByDiscordId(String discordId) {
        return storage.getPlayerByDiscordId(discordId);
    }
    public UUID getPlayerByTelegramId(String telegramId) {
        return storage.getPlayerByTelegramId(telegramId);
    }
    public String getPlayerName(UUID playerUUID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        return offlinePlayer.getName();
    }

    public String getDiscordId(UUID playerUUID) {
        if ("discord".equalsIgnoreCase(storage.getBindingType(playerUUID))) {
            return storage.getBindingId(playerUUID);
        }
        return null;
    }

    public String getDiscordIdByPlayer(UUID playerUUID) {
        String bindingType = storage.getBindingType(playerUUID);
        if ("discord".equals(bindingType)) {
            return storage.getBindingId(playerUUID);
        }
        return null;
    }

    public boolean isAccountBound(UUID playerUUID) {
        return accountManager.isAccountBound(playerUUID);
    }
    public String getBindingType(UUID playerUUID) {
        return accountManager.getBindingType(playerUUID);
    }
    public UUID getPlayerUUIDByName(String playerName) {
        @SuppressWarnings("deprecation")
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return player.getUniqueId();
    }
    public AccountStorage getAccountStorage() {
        return storage;
    }
}

