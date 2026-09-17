package z.caAuth.account;

import org.bukkit.entity.Player;

import java.util.UUID;

public class AccountManager {
    private final AccountStorage storage;
    public AccountManager(AccountStorage storage) {
        this.storage = storage;
    }
    public boolean isAccountBound(UUID playerUUID) {
        return storage.isBound(playerUUID);
    }
    public boolean isAccountBound(Player player) {
        return isAccountBound(player.getUniqueId());
    }
    public void bindDiscord(UUID playerUUID, String discordId) {
        storage.bindDiscord(playerUUID, discordId);
    }
    public void bindTelegram(UUID playerUUID, String telegramId) {
        storage.bindTelegram(playerUUID, telegramId);
    }
    public String getBindingType(UUID playerUUID) {
        return storage.getBindingType(playerUUID);
    }
    public String getBindingId(UUID playerUUID) {
        return storage.getBindingId(playerUUID);
    }
    public void unbindAccount(UUID playerUUID) {
        storage.unbind(playerUUID);
    }
    public String generateVerificationCode(UUID playerUUID) {
        return storage.generateVerificationCode(playerUUID);
    }
    public boolean verifyCode(UUID playerUUID, String code) {
        return storage.verifyCode(playerUUID, code);
    }
    public long getLastConfirmedAt(UUID playerUUID) {
        return storage.getLastConfirmedAt(playerUUID);
    }
    public void setLastConfirmedAt(UUID playerUUID, long epochSeconds) {
        storage.setLastConfirmedAt(playerUUID, epochSeconds);
    }
}

