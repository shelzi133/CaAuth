package z.caAuth.bots.discord.role;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.account.AccountManager;
import z.caAuth.bots.discord.DiscordBot;
import z.caAuth.config.RolesConfig;
import z.caAuth.integration.plan.PlaytimeProvider;

import java.util.UUID;
import java.util.function.Supplier;

public class ActiveRoleSyncScheduler implements Listener {

    private static final long PLAN_SAVE_DELAY_TICKS = 100L;

    private final JavaPlugin plugin;
    private final PlaytimeProvider playtimeProvider;
    private final RolesConfig rolesConfig;
    private final AccountManager accountManager;
    private final Supplier<DiscordBot> discordBot;

    public ActiveRoleSyncScheduler(JavaPlugin plugin, AccountManager accountManager, Supplier<DiscordBot> discordBot,
                                   RolesConfig rolesConfig, PlaytimeProvider playtimeProvider) {
        this.plugin = plugin;
        this.discordBot = discordBot;
        this.rolesConfig = rolesConfig;
        this.accountManager = accountManager;
        this.playtimeProvider = playtimeProvider;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        if (!rolesConfig.getActivePlayerRole()) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> check(uuid), PLAN_SAVE_DELAY_TICKS);
    }

    private void check(UUID uuid) {
        DiscordBot bot = discordBot.get();
        if (bot == null || !bot.isRunning()) {
            return;
        }
        if (!rolesConfig.getActivePlayerRole()) {
            return;
        }
        if (accountManager.isActiveRoleOverridden(uuid)) {
            return;
        }

        long playtimeMillis;
        try {
            playtimeMillis = playtimeProvider.getPlaytimeLastTime(uuid);
        } catch (IllegalStateException planNotReady) {
            return;
        }

        boolean shouldHaveRole = playtimeMillis >= rolesConfig.getRequiredTime();
        bot.getRoleService().syncActiveRole(uuid, shouldHaveRole);
    }
}
