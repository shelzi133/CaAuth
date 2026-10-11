package z.caAuth.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import z.caAuth.account.AccountManager;
import z.caAuth.account.BindingType;
import z.caAuth.binding.BindingService;
import z.caAuth.config.AuthMessagesConfig;

import java.util.UUID;

public class UnbindCommand {
    private final AccountManager accountManager;
    private final BindingService bindingService;
    private final AuthMessagesConfig authMessagesConfig;

    public UnbindCommand(AccountManager accountManager, BindingService bindingService, AuthMessagesConfig authMessagesConfig) {
        this.accountManager = accountManager;
        this.bindingService = bindingService;
        this.authMessagesConfig = authMessagesConfig;
    }

    @Command("unbind")
    @CommandPermission("caauth.unbind")
    public void unbind(BukkitCommandActor actor, @Optional OfflinePlayer target) {
        CommandSender sender = actor.sender();
        if (target == null) {
            sender.sendMessage(authMessagesConfig.getCaauthUnbindingUsage());
            return;
        }

        UUID playerUUID = target.getUniqueId();
        String playerName = target.getName() != null ? target.getName() : playerUUID.toString();
        if (!accountManager.isAccountBound(playerUUID)) {
            sender.sendMessage(authMessagesConfig.getCaauthUnbindingNoBound(playerName));
            return;
        }

        BindingType bindingType = accountManager.getBindingType(playerUUID);
        String bindingId = accountManager.getBindingId(playerUUID);
        bindingService.unbindAccount(playerUUID);
        sender.sendMessage(authMessagesConfig.getCaauthUnbindingSuccess(playerName));
        sender.sendMessage(authMessagesConfig.getCaauthUnbindingType() + (bindingType != null ? bindingType.getDisplayName() : "?") + " §7(ID: §f" + bindingId + "§7)");
    }
}
