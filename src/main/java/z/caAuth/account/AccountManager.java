package z.caAuth.account;

import org.bukkit.entity.Player;
import z.caAuth.config.AuthConfig;

import java.util.List;
import java.util.Locale;
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

    public boolean isNameBound(String playerName) {
        return storage.getBoundPlayerByName(playerName) != null;
    }

    public boolean isTwink(UUID playerUUID) {
        return storage.getOwner(playerUUID) != null;
    }

    public List<AccountRepository.TwinkRow> getTwinks(UUID ownerUUID) {
        return storage.getTwinks(ownerUUID);
    }

    public void bindTwink(UUID twinkUUID, UUID ownerUUID, BindingType type, String bindingId, String twinkName) {
        storage.bindTwink(twinkUUID, ownerUUID, type.getId(), bindingId, twinkName);
    }

    public void deleteTwinks(UUID ownerUUID) {
        storage.deleteTwinks(ownerUUID);
        storage.deleteInvites(ownerUUID);
    }

    public void createInvite(String twinkName, UUID ownerUUID, String code) {
        storage.createInvite(twinkName.toLowerCase(Locale.ROOT), twinkName, ownerUUID, code,
                System.currentTimeMillis() / 1000L);
    }

    public AccountRepository.Invite findInvite(String twinkName) {
        return storage.findInvite(twinkName.toLowerCase(Locale.ROOT), inviteNotBefore());
    }

    public List<AccountRepository.Invite> getInvites(UUID ownerUUID) {
        return storage.getInvites(ownerUUID, inviteNotBefore());
    }

    public boolean isInviteCodeTaken(String code) {
        return storage.isInviteCodeTaken(code, inviteNotBefore());
    }

    public void deleteInvite(String twinkName) {
        storage.deleteInvite(twinkName.toLowerCase(Locale.ROOT));
    }

    public List<SkinProfile> getSkinProfiles(UUID playerUUID) {
        return storage.getSkinProfiles(playerUUID);
    }

    public SkinProfile getSkinProfile(UUID playerUUID, int slot) {
        return storage.getSkinProfile(playerUUID, slot);
    }

    public void saveSkinProfile(UUID playerUUID, SkinProfile profile) {
        storage.saveSkinProfile(playerUUID, profile);
    }

    public void deleteSkinProfile(UUID playerUUID, int slot) {
        storage.deleteSkinProfile(playerUUID, slot);
    }

    private long inviteNotBefore() {
        long lifetimeSeconds = Math.max(1, authConfig.bots.twink.inviteLifetimeMinutes) * 60L;
        return System.currentTimeMillis() / 1000L - lifetimeSeconds;
    }
}
