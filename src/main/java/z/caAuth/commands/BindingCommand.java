package z.caAuth.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import z.caAuth.account.AccountManager;
import z.caAuth.config.MessagesConfig;
import z.caAuth.util.BindingType;
import z.caAuth.util.ColorManager;

public class BindingCommand implements CommandExecutor {
    private final AccountManager accountManager;
    private final MessagesConfig messagesConfig;
    public BindingCommand(AccountManager accountManager, MessagesConfig messagesConfig) {
        this.accountManager = accountManager;
        this.messagesConfig = messagesConfig;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messagesConfig.getCommandPlayerOnly());
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            if (accountManager.isAccountBound(player)) {
                String type = accountManager.getBindingType(player.getUniqueId());
                BindingType bindingType = BindingType.fromString(type);
                String typeName = bindingType != null ? bindingType.getDisplayName() : type;
                ColorManager.sendMessage(player, messagesConfig.getSuccessPrefix() + "Ваш аккаунт привязан через " + typeName);
            } else {
                ColorManager.sendMessage(player, messagesConfig.getAccountNotBound());
                ColorManager.sendMessage(player, messagesConfig.getBindingInstructions());
                ColorManager.sendMessage(player, messagesConfig.getBindingDiscord());
                ColorManager.sendMessage(player, messagesConfig.getBindingTelegram());
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("code")) {
            if (accountManager.isAccountBound(player)) {
                ColorManager.sendMessage(player, messagesConfig.getAccountAlreadyBound());
                return true;
            }
            String code = accountManager.generateVerificationCode(player.getUniqueId());
            ColorManager.sendMessage(player, messagesConfig.getVerificationCodeGenerated(code));
            ColorManager.sendMessage(player, messagesConfig.getVerificationCodeInstructions());
            return true;
        }
        ColorManager.sendMessage(player, messagesConfig.getCommandUsage());
        return true;
    }
}

