package z.caAuth.bots.discord.role;

import z.caAuth.account.AccountManager;
import z.caAuth.bots.discord.DiscordBot;

import java.util.UUID;
import java.util.function.Supplier;

public class ActiveRoleService {

    private final AccountManager accountManager;
    private final Supplier<DiscordBot> discordBot;

    public ActiveRoleService(AccountManager accountManager, Supplier<DiscordBot> discordBot) {
        this.accountManager = accountManager;
        this.discordBot = discordBot;
    }

    public boolean isActiveRole(UUID playerUUID) {
        return accountManager.isActiveRoleOverridden(playerUUID);
    }

    public boolean isDiscordAvailable() {
        DiscordBot bot = discordBot.get();
        return bot != null && bot.isRunning();
    }

    public void grantActiveRole(UUID playerUUID) {
        setActiveRole(playerUUID, true);
    }

    public void removeActiveRole(UUID playerUUID) {
        setActiveRole(playerUUID, false);
    }

    private void setActiveRole(UUID playerUUID, boolean active) {
        DiscordBot bot = discordBot.get();
        if (bot == null || !bot.isRunning()) {
            return;
        }
        accountManager.setActiveRoleOverridden(playerUUID, active);
        DiscordRoleService roleService = bot.getRoleService();
        roleService.syncActiveRole(playerUUID, active);
    }
}
