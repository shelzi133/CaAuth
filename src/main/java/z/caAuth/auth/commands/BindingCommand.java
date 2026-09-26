package z.caAuth.auth.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import z.caAuth.auth.account.AccountManager;
import z.caAuth.auth.config.AuthMessagesConfig;
import z.caAuth.auth.util.BindingType;
import z.caAuth.auth.util.ColorManager;
import z.caAuth.core.config.MainConfig;

public class BindingCommand implements CommandExecutor {

    private final AccountManager accountManager;
    private final AuthMessagesConfig authMessagesConfig;
    private final MainConfig mainConfig;


    public BindingCommand(AccountManager accountManager, AuthMessagesConfig authMessagesConfig, MainConfig mainConfig) {
        this.accountManager = accountManager;
        this.authMessagesConfig = authMessagesConfig;
        this.mainConfig = mainConfig;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(authMessagesConfig.getCommandPlayerOnly());
            return true;
        }
        Player player = (Player) sender;

        if (!mainConfig.module.auth.enabled){
            ColorManager.sendMessage(player, authMessagesConfig.getModuleAuthDisabled());
            return true;
        }

        if (args.length == 0) {
            if (accountManager.isAccountBound(player)) {
                String type = accountManager.getBindingType(player.getUniqueId());
                BindingType bindingType = BindingType.fromString(type);
                String typeName = bindingType != null ? bindingType.getDisplayName() : type;
                ColorManager.sendMessage(player, authMessagesConfig.getSuccessPrefix() + "Ваш аккаунт привязан через " + typeName);
            } else {
                ColorManager.sendMessage(player, authMessagesConfig.getAccountNotBound());
                ColorManager.sendMessage(player, authMessagesConfig.getBindingInstructions());
                ColorManager.sendMessage(player, authMessagesConfig.getBindingDiscord());
                ColorManager.sendMessage(player, authMessagesConfig.getBindingTelegram());
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("code")) {
            if (accountManager.isAccountBound(player)) {
                ColorManager.sendMessage(player, authMessagesConfig.getAccountAlreadyBound());
                return true;
            }
            String code = accountManager.generateVerificationCode(player.getUniqueId());
            ColorManager.sendMessage(player, authMessagesConfig.getVerificationCodeGenerated(code));
            ColorManager.sendMessage(player, authMessagesConfig.getVerificationCodeInstructions());
            return true;
        }
        ColorManager.sendMessage(player, authMessagesConfig.getCommandUsage());
        return true;
    }
}

