package z.caAuth.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import z.caAuth.config.AuthMessagesConfig;
import z.caAuth.config.SkinsConfig;
import z.caAuth.skins.SkinService;
import z.caAuth.skins.SkinsGui;
import z.caAuth.util.Colors;

public class SkinsCommand {

    private static final String ADMIN_PERMISSION = "caauth.admin";

    private final SkinsGui gui;
    private final SkinService skins;
    private final SkinsConfig config;
    private final AuthMessagesConfig authMessages;

    public SkinsCommand(SkinsGui gui, SkinService skins, SkinsConfig config, AuthMessagesConfig authMessages) {
        this.gui = gui;
        this.skins = skins;
        this.config = config;
        this.authMessages = authMessages;
    }

    @Command("skins")
    public void open(BukkitCommandActor actor, @Optional OfflinePlayer target) {
        CommandSender sender = actor.sender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(authMessages.getErrorPrefix() + Colors.colorize(config.messages.playerOnly));
            return;
        }
        if (!skins.isAvailable()) {
            sender.sendMessage(authMessages.getErrorPrefix() + Colors.colorize(config.messages.unavailable));
            return;
        }
        if (target == null || target.getUniqueId().equals(player.getUniqueId())) {
            gui.openProfiles(player);
            return;
        }
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(authMessages.getNoPermission());
            return;
        }
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(authMessages.getErrorPrefix()
                    + Colors.colorize(config.messages.playerNotFound.replace("{player}", String.valueOf(target.getName()))));
            return;
        }
        gui.openProfiles(player, target.getUniqueId());
    }
}
