package z.caAuth.bots.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import z.caAuth.account.BindingType;
import z.caAuth.binding.BindingService;
import z.caAuth.binding.BindingService.RedeemResult;
import z.caAuth.bots.discord.role.DiscordRoleService;
import z.caAuth.config.DiscordBotMessages;
import z.caAuth.config.DiscordBotMessages.EmbedType;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;


public class DiscordMessageHandler {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    private final BindingService bindingService;
    private final DiscordBotMessages messagesConfig;
    private final DiscordRoleService roleService;
    private final DiscordEmbeds embeds;
    private final DiscordMenu menu;
    private final BooleanSupplier velocityMode;

    public DiscordMessageHandler(BindingService bindingService, DiscordBotMessages messagesConfig,
                                 DiscordRoleService roleService, DiscordEmbeds embeds, DiscordMenu menu,
                                 BooleanSupplier velocityMode) {
        this.velocityMode = velocityMode;
        this.bindingService = bindingService;
        this.messagesConfig = messagesConfig;
        this.roleService = roleService;
        this.embeds = embeds;
        this.menu = menu;
    }

    public void handlePrivateMessage(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw().trim();
        String discordId = event.getAuthor().getId();

        UUID existingPlayerUUID = bindingService.getPlayerByBinding(BindingType.DISCORD, discordId);
        if (existingPlayerUUID != null) {
            String playerName = resolvePlayerName(existingPlayerUUID);
            send(event, EmbedType.INFO, messagesConfig.getDiscordAlreadyBound(playerName));
            return;
        }

        if (CODE_PATTERN.matcher(message).matches()) {
            handleVerificationCode(event, message, discordId);
        } else {
            send(event, EmbedType.INFO, messagesConfig.getDiscordInstructions(velocityMode.getAsBoolean()));
        }
    }

    private void handleVerificationCode(MessageReceivedEvent event, String code, String discordId) {
        RedeemResult result = bindingService.redeemCode(BindingType.DISCORD, discordId, code);
        switch (result.status()) {
            case BOUND -> {
                String playerName = resolvePlayerName(result.playerUUID());
                send(event, EmbedType.SUCCESS, messagesConfig.getDiscordBindingSuccess(playerName));
                menu.sendMenu(event.getChannel());
                roleService.assignRoleAndNicknameIfEnabled(discordId, result.playerUUID(), playerName);
            }
            case INVALID_CODE -> send(event, EmbedType.ERROR, messagesConfig.getDiscordInvalidCode());
            case LOCKED -> send(event, EmbedType.WARNING,
                    messagesConfig.getDiscordTooManyAttempts(result.lockedMinutes()));
            case ALREADY_BOUND -> send(event, EmbedType.ERROR,
                    messagesConfig.getDiscordCodeUsed(resolvePlayerName(result.playerUUID())));
            case BINDING_FAILED -> send(event, EmbedType.ERROR, messagesConfig.getDiscordBindingError());
        }
    }

    private String resolvePlayerName(UUID playerUUID) {
        String name = bindingService.getPlayerName(playerUUID);
        return name != null && !name.isBlank() ? name : playerUUID.toString();
    }

    private void send(MessageReceivedEvent event, EmbedType type, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        event.getChannel().sendMessageEmbeds(embeds.embed(type, text)).queue();
    }
}
