package z.caAuth.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import z.caAuth.bots.discord.role.ActiveRoleService;
import z.caAuth.config.AuthMessagesConfig;

import java.util.UUID;

public class ActivityPlayerCommand {

    private static final String PERMISSION_SELF = "caauth.activityplayer.self";
    private static final String PERMISSION_OTHER = "caauth.activityplayer.other";

    private final ActiveRoleService activeRoleService;
    private final AuthMessagesConfig messagesConfig;

    public ActivityPlayerCommand(ActiveRoleService activeRoleService, AuthMessagesConfig messagesConfig) {
        this.activeRoleService = activeRoleService;
        this.messagesConfig = messagesConfig;
    }

    @Command("activityplayer")
    public void toggle(BukkitCommandActor actor, @Optional OfflinePlayer target) {
        if (target == null) {
            handleSelf(actor.sender());
        } else {
            handleOther(actor.sender(), target);
        }
    }

    private void handleSelf(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messagesConfig.getCommandPlayerOnly());
            return;
        }
        if (!sender.hasPermission(PERMISSION_SELF)) {
            sender.sendMessage(messagesConfig.getNoPermission());
            return;
        }
        if (!activeRoleService.isDiscordAvailable()) {
            sender.sendMessage(messagesConfig.getBotUnavailable());
            return;
        }

        if (toggleRole(player.getUniqueId())) {
            sender.sendMessage(messagesConfig.getIssuedActivityPlayerRoleSelf());
        } else {
            sender.sendMessage(messagesConfig.getTakeActivityPlayerRoleSelf());
        }
    }

    private void handleOther(CommandSender sender, OfflinePlayer target) {
        if (!sender.hasPermission(PERMISSION_OTHER)) {
            sender.sendMessage(messagesConfig.getNoPermission());
            return;
        }
        if (!activeRoleService.isDiscordAvailable()) {
            sender.sendMessage(messagesConfig.getBotUnavailable());
            return;
        }

        String playerName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        if (toggleRole(target.getUniqueId())) {
            sender.sendMessage(messagesConfig.getIssuedActivityPlayerRoleOther(playerName));
        } else {
            sender.sendMessage(messagesConfig.getTakeActivityPlayerRoleOther(playerName));
        }
    }

    private boolean toggleRole(UUID playerUUID) {
        if (activeRoleService.isActiveRole(playerUUID)) {
            activeRoleService.removeActiveRole(playerUUID);
            return false;
        }
        activeRoleService.grantActiveRole(playerUUID);
        return true;
    }
}
