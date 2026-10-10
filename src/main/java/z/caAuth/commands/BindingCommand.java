package z.caAuth.commands;

import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Subcommand;
import z.caAuth.account.AccountManager;
import z.caAuth.account.BindingType;
import z.caAuth.config.AuthMessagesConfig;
import z.caAuth.config.MainConfig;
import z.caAuth.util.Colors;

@Command("binding")
public class BindingCommand {

    private final AccountManager accountManager;
    private final AuthMessagesConfig authMessagesConfig;
    private final MainConfig mainConfig;

    public BindingCommand(AccountManager accountManager, AuthMessagesConfig authMessagesConfig, MainConfig mainConfig) {
        this.accountManager = accountManager;
        this.authMessagesConfig = authMessagesConfig;
        this.mainConfig = mainConfig;
    }

    @CommandPlaceholder
    public void status(Player player) {
        if (isAuthDisabled(player)) {
            return;
        }
        if (accountManager.isAccountBound(player)) {
            BindingType bindingType = accountManager.getBindingType(player.getUniqueId());
            Colors.sendMessage(player, bindingType == BindingType.TELEGRAM
                    ? authMessagesConfig.getAccountBoundTelegram()
                    : authMessagesConfig.getAccountBoundDiscord());
            return;
        }
        Colors.sendMessage(player, authMessagesConfig.getAccountNotBound());
        Colors.sendMessage(player, authMessagesConfig.getBindingInstructions());
        Colors.sendMessage(player, authMessagesConfig.getBindingDiscord());
        Colors.sendMessage(player, authMessagesConfig.getBindingTelegram());
    }

    @Subcommand("code")
    public void code(Player player) {
        if (isAuthDisabled(player)) {
            return;
        }
        if (accountManager.isAccountBound(player)) {
            Colors.sendMessage(player, authMessagesConfig.getAccountAlreadyBound());
            return;
        }
        String code = accountManager.generateVerificationCode(player.getUniqueId(), player.getName());
        Colors.sendMessage(player, authMessagesConfig.getVerificationCodeGenerated(code));
        Colors.sendMessage(player, authMessagesConfig.getVerificationCodeInstructions());
    }

    private boolean isAuthDisabled(Player player) {
        if (mainConfig.module.auth.enabled) {
            return false;
        }
        Colors.sendMessage(player, authMessagesConfig.getModuleAuthDisabled());
        return true;
    }
}
