package z.caAuth.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import z.caAuth.auth.config.AuthMessagesConfig;
import z.caAuth.bots.discord.Role.Manager.RoleManager;

import java.util.UUID;

public class ActivityPlayerCommand  {

    private RoleManager roleManager;
    private final AuthMessagesConfig messagesConfig;

    public ActivityPlayerCommand(RoleManager roleManager, AuthMessagesConfig messagesConfig){
        this.messagesConfig = messagesConfig;
        this.roleManager = roleManager;
    }

    @Command("activityplayer")
    @Permission("caauth.activityplayer.self")
    public void activityPlayerSelf(Player sender){
        UUID playeruuid = sender.getUniqueId();


        if(!roleManager.isDiscordAvailable()) {
           sender.sendMessage(messagesConfig.getBotUnavailable());
           return;
        }

        if(roleManager.isActiveRole(playeruuid) == false) {
            roleManager.grantActiveRole(playeruuid);
            sender.sendMessage(messagesConfig.getIssuedActivityPlayerRoleSelf());
        } else {
            roleManager.removeActiveRole(playeruuid);
            sender.sendMessage(messagesConfig.getTakeActivityPlayerRoleSelf());
        }
    }

    @Command("activityplayer <target>")
    @Permission("caauth.activityplayer.other")
    public void activityPlayerOther(CommandSender sender, @Argument("target") Player target){


        if(!roleManager.isDiscordAvailable()) {
            sender.sendMessage(messagesConfig.getBotUnavailable());
            return;
        }

        UUID playeruuid = target.getUniqueId();
        String playername = target.getName();

        if(roleManager.isActiveRole(playeruuid) == false) {
            roleManager.grantActiveRole(playeruuid);
            sender.sendMessage(messagesConfig.getIssuedActivityPlayerRoleOther(playername));
        } else {
            roleManager.removeActiveRole(playeruuid);
            sender.sendMessage(messagesConfig.getTakeActivityPlayerRoleOther(playername));
        }
    }

}

