package z.caAuth.auth.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import z.caAuth.auth.account.AccountManager;
import z.caAuth.auth.config.AuthMessagesConfig;
import z.caAuth.auth.util.ColorManager;

import java.util.UUID;


public class UnbindCommand implements CommandExecutor {
    private final AccountManager accountManager;
    private final AuthMessagesConfig authMessagesConfig;

    public UnbindCommand(AccountManager accountManager, AuthMessagesConfig authMessagesConfig) {
        this.accountManager = accountManager;
        this.authMessagesConfig = authMessagesConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("caauth.unbind")) {
            sender.sendMessage(authMessagesConfig.getNoPermission());
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(authMessagesConfig.getCaauthUnBindingUsage());
            return true;
        }

        String playerName = args[0];

        Player onlineTarget = Bukkit.getPlayerExact(playerName);

        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer targetPlayer = onlineTarget != null
                ? onlineTarget
                : Bukkit.getOfflinePlayer(playerName);

        if (onlineTarget == null && !targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(authMessagesConfig.getPlayerNeverPlayed(playerName));
            return true;
        }
        UUID playerUUID = targetPlayer.getUniqueId();
        if (!accountManager.isAccountBound(playerUUID)) {
            sender.sendMessage(authMessagesConfig.getCaauthUnBindingNoBound(playerName));
            return true;
        }
        String bindingType = accountManager.getBindingType(playerUUID);
        String bindingId = accountManager.getBindingId(playerUUID);
        accountManager.unbindAccount(playerUUID);
        sender.sendMessage(authMessagesConfig.getCaauthUnBindingSuccess(playerName));
        sender.sendMessage(authMessagesConfig.getCaauthUnBindingType() + bindingType + " §7(ID: §f" + bindingId + "§7)");
        Player onlinePlayer = targetPlayer.getPlayer();
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            ColorManager.sendMessage(onlinePlayer, authMessagesConfig.getCaauthUnBindingNotifyPlayer(playerName));
            ColorManager.sendMessage(onlinePlayer, authMessagesConfig.getAccountNotBound());
            ColorManager.sendMessage(onlinePlayer, authMessagesConfig.getBindingInstructions());
        }

        return true;
    }
}