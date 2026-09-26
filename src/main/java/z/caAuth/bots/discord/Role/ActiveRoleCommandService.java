package z.caAuth.bots.discord.Role;

import org.bukkit.entity.Player;
import z.caAuth.auth.account.AccountStorage;
import z.caAuth.bots.discord.DiscordBot;

import java.util.UUID;

public class ActiveRoleCommandService {

    private final AccountStorage storage;
    private final DiscordBot discordBot;

    public ActiveRoleCommandService(AccountStorage storage, DiscordBot discordBot) {
        this.storage = storage;
        this.discordBot = discordBot;
    }

    public boolean isActiveRole(UUID playerUUID) {
        return storage.isActiveRole(playerUUID);
    }

    public boolean isActiveRole(Player player) {
        return isActiveRole(player.getUniqueId());
    }
    public void setOverridden(UUID playerUUID, boolean overridden) {
        storage.setOverridden(playerUUID, overridden);
    }

    public boolean isDiscordAvailable(){
        if (discordBot == null) {
            return false;
        }
        return true;
    }

    public void grantActiveRole(UUID playerUUID) {
        if (!isDiscordAvailable()) {
            return;
        }
        setOverridden(playerUUID, true);
        discordBot.getRoleService().syncActiveRole(playerUUID, true);
    }

    public void removeActiveRole(UUID playerUUID) {
        if (!isDiscordAvailable()) {
            return;
        }
        setOverridden(playerUUID, false);
        discordBot.getRoleService().syncActiveRole(playerUUID, false);
    }

    public void resetToAutomatic(UUID playerUUID) {
        setOverridden(playerUUID, false);
    }
}
