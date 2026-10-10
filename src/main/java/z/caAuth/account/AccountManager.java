package z.caAuth.account;

import org.bukkit.entity.Player;
import z.caAuth.config.AuthConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AccountManager {
    private final AccountRepository storage;
    private final AuthConfig authConfig;

    public AccountManager(AccountRepository storage, AuthConfig authConfig) {
        this.storage = storage;
        this.authConfig = authConfig;
    }

    public boolean isAccountBound(UUID playerUUID) {
        return storage.isBound(playerUUID);
    }

    public boolean isAccountBound(Player player) {
        return isAccountBound(player.getUniqueId());
    }

    public void bind(UUID playerUUID, BindingType type, String bindingId) {
        storage.bind(playerUUID, type.getId(), bindingId);
    }

    public BindingType getBindingType(UUID playerUUID) {
        return BindingType.fromString(storage.getBindingType(playerUUID));
    }

    public String getBindingId(UUID playerUUID) {
        return storage.getBindingId(playerUUID);
    }

    public String getBindingId(UUID playerUUID, BindingType type) {
        return getBindingType(playerUUID) == type ? storage.getBindingId(playerUUID) : null;
    }

    public UUID getPlayerByBinding(BindingType type, String bindingId) {
        return storage.getPlayerByBinding(type.getId(), bindingId);
    }

    public Map<UUID, String> getBindingsByType(BindingType type) {
        return storage.getBindingsByType(type.getId());
    }

    public List<String> getRegisteredPlayerNames() {
        return storage.getRegisteredPlayerNames();
    }

    public void unbindAccount(UUID playerUUID) {
        storage.unbind(playerUUID);
    }

    public String generateVerificationCode(UUID playerUUID, String playerName) {
        return storage.generateVerificationCode(playerUUID, playerName, codeNotBefore());
    }

    public UUID consumePendingCode(String code) {
        return storage.consumePendingCode(code, codeNotBefore());
    }

    private long codeNotBefore() {
        long lifetimeSeconds = Math.max(1, authConfig.bots.bindingCode.lifetimeMinutes) * 60L;
        return System.currentTimeMillis() / 1000L - lifetimeSeconds;
    }

    public String getPlayerName(UUID playerUUID) {
        return storage.getPlayerName(playerUUID);
    }

    public void setPlayerName(UUID playerUUID, String playerName) {
        storage.setPlayerName(playerUUID, playerName);
    }

    public long getLastConfirmedAt(UUID playerUUID) {
        return storage.getLastConfirmedAt(playerUUID);
    }

    public String getLastConfirmedIp(UUID playerUUID) {
        return storage.getLastConfirmedIp(playerUUID);
    }

    public void markConfirmed(UUID playerUUID, String ip) {
        storage.setLastConfirmedAt(playerUUID, System.currentTimeMillis() / 1000L);
        if (ip != null) {
            storage.setLastConfirmedIp(playerUUID, ip);
        }
    }

    public boolean isActiveRoleOverridden(UUID playerUUID) {
        return storage.isActiveRole(playerUUID);
    }

    public void setActiveRoleOverridden(UUID playerUUID, boolean overridden) {
        storage.setOverridden(playerUUID, overridden);
    }
}
