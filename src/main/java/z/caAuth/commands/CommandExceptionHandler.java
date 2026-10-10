package z.caAuth.commands;

import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler;
import revxrsal.commands.bukkit.exception.InvalidPlayerException;
import revxrsal.commands.bukkit.exception.SenderNotPlayerException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.exception.RuntimeExceptionAdapter.HandleException;
import z.caAuth.config.AuthMessagesConfig;

public class CommandExceptionHandler extends BukkitExceptionHandler {

    private final AuthMessagesConfig messagesConfig;

    public CommandExceptionHandler(AuthMessagesConfig messagesConfig) {
        this.messagesConfig = messagesConfig;
    }

    @Override
    @HandleException
    public void onSenderNotPlayer(SenderNotPlayerException e, BukkitCommandActor actor) {
        actor.sender().sendMessage(messagesConfig.getCommandPlayerOnly());
    }

    @Override
    @HandleException
    public void onNoPermission(NoPermissionException e, BukkitCommandActor actor) {
        actor.sender().sendMessage(messagesConfig.getNoPermission());
    }

    @Override
    @HandleException
    public void onInvalidPlayer(InvalidPlayerException e, BukkitCommandActor actor) {
        actor.sender().sendMessage(messagesConfig.getPlayerNeverPlayed(e.input()));
    }
}
