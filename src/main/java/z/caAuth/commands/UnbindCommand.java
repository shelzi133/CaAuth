package z.caAuth.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import z.caAuth.account.AccountManager;
import z.caAuth.config.MessagesConfig;
import z.caAuth.util.ColorManager;

import java.util.UUID;


public class UnbindCommand implements CommandExecutor {
    private final AccountManager accountManager;
    private final MessagesConfig messagesConfig;

    public UnbindCommand(AccountManager accountManager, MessagesConfig messagesConfig) {
        this.accountManager = accountManager;
        this.messagesConfig = messagesConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("caauth.unbind")) {
            sender.sendMessage(messagesConfig.getNoPermission());
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(messagesConfig.getCaauthUnBindingUsage());
            return true;
        }

        String playerName = args[0];

        Player onlineTarget = Bukkit.getPlayerExact(playerName);

        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer targetPlayer = onlineTarget != null
                ? onlineTarget
                : Bukkit.getOfflinePlayer(playerName);

        if (onlineTarget == null && !targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(messagesConfig.getPlayerNeverPlayed(playerName));
            return true;
        }
        UUID playerUUID = targetPlayer.getUniqueId();
        if (!accountManager.isAccountBound(playerUUID)) {
            sender.sendMessage(messagesConfig.getCaauthUnBindingNoBound(playerName));
            return true;
        }
        String bindingType = accountManager.getBindingType(playerUUID);
        String bindingId = accountManager.getBindingId(playerUUID);
        accountManager.unbindAccount(playerUUID);
        sender.sendMessage(messagesConfig.getCaauthUnBindingSuccess(playerName));
        sender.sendMessage(messagesConfig.getCaauthUnBindingType() + bindingType + " §7(ID: §f" + bindingId + "§7)");
        Player onlinePlayer = targetPlayer.getPlayer();
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            ColorManager.sendMessage(onlinePlayer, messagesConfig.getCaauthUnBindingNotifyPlayer(playerName));
            ColorManager.sendMessage(onlinePlayer, messagesConfig.getAccountNotBound());
            ColorManager.sendMessage(onlinePlayer, messagesConfig.getBindingInstructions());
        }

        return true;
    }
}