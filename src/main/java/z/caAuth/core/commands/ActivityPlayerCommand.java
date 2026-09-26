package z.caAuth.core.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import z.caAuth.auth.config.AuthMessagesConfig;
import z.caAuth.bots.discord.Role.ActiveRoleCommandService;

import java.util.UUID;

public class ActivityPlayerCommand  {

    private ActiveRoleCommandService activeRoleCommandService;
    private final AuthMessagesConfig messagesConfig;

    public ActivityPlayerCommand(ActiveRoleCommandService activeRoleCommandService, AuthMessagesConfig messagesConfig){
        this.messagesConfig = messagesConfig;
        this.activeRoleCommandService = activeRoleCommandService;
    }

    @Command("activityplayer")
    @Permission("caauth.activityplayer.self")
    public void activityPlayerSelf(Player sender){
        UUID playeruuid = sender.getUniqueId();


        if(!activeRoleCommandService.isDiscordAvailable()) {
           sender.sendMessage(messagesConfig.getBotUnavailable());
           return;
        }

        if(activeRoleCommandService.isActiveRole(playeruuid) == false) {
            activeRoleCommandService.grantActiveRole(playeruuid);
            sender.sendMessage(messagesConfig.getIssuedActivityPlayerRoleSelf());
        } else {
            activeRoleCommandService.removeActiveRole(playeruuid);
            sender.sendMessage(messagesConfig.getTakeActivityPlayerRoleSelf());
        }
    }

    @Command("activityplayer <target>")
    @Permission("caauth.activityplayer.other")
    public void activityPlayerOther(CommandSender sender, @Argument("target") Player target){


        if(!activeRoleCommandService.isDiscordAvailable()) {
            sender.sendMessage(messagesConfig.getBotUnavailable());
            return;
        }

        UUID playeruuid = target.getUniqueId();
        String playername = target.getName();

        if(activeRoleCommandService.isActiveRole(playeruuid) == false) {
            activeRoleCommandService.grantActiveRole(playeruuid);
            sender.sendMessage(messagesConfig.getIssuedActivityPlayerRoleOther(playername));
        } else {
            activeRoleCommandService.removeActiveRole(playeruuid);
            sender.sendMessage(messagesConfig.getTakeActivityPlayerRoleOther(playername));
        }
    }

}

