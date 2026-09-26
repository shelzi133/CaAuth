package z.caAuth.bots.discord.Role;

import org.bukkit.event.Listener;
import z.caAuth.auth.account.AccountStorage;
import z.caAuth.bots.discord.DiscordBot;
import z.caAuth.bots.discord.role.config.RolesConfig;
import z.caAuth.hook.TimeCounter;

import java.util.List;
import java.util.UUID;

public class ActiveRoleSyncScheduler implements Listener {

    private final TimeCounter timeCounter;
    private final RolesConfig rolesConfig;
    private final AccountStorage storage;
    private final DiscordBot discordBot;

    public ActiveRoleSyncScheduler(AccountStorage storage, DiscordBot discordBot, RolesConfig rolesConfig, TimeCounter timeCounter) {
        this.discordBot = discordBot;
        this.rolesConfig = rolesConfig;
        this.storage = storage;
        this.timeCounter = timeCounter;
    }

    public void checkAll() {
        if (!rolesConfig.getActivePlayerRole()) {
            return;
        }
        List<UUID> players = storage.getAllBoundDiscordPlayers();

        for (UUID uuid : players) {
            if (storage.isActiveRole(uuid)) {
                continue;
            }
            long weeklyMillis = timeCounter.getPlaytimeLastTime(uuid);

            if (weeklyMillis >= rolesConfig.getRequiredTime()) {
                discordBot.getRoleService().syncActiveRole(uuid, true);
            } else {
                discordBot.getRoleService().syncActiveRole(uuid, false);
            }
        }
    }
}

