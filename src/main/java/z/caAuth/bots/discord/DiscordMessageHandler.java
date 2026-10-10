package z.caAuth.bots.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import z.caAuth.account.BindingType;
import z.caAuth.binding.BindingService;
import z.caAuth.binding.BindingService.RedeemResult;
import z.caAuth.bots.discord.role.DiscordRoleService;
import z.caAuth.config.DiscordBotMessages;
import z.caAuth.util.Colors;

import java.awt.Color;
import java.util.UUID;
import java.util.regex.Pattern;

public class DiscordMessageHandler {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    private final BindingService bindingService;
    private final DiscordBotMessages messagesConfig;
    private final DiscordRoleService roleService;

    public DiscordMessageHandler(BindingService bindingService, DiscordBotMessages messagesConfig,
                                 DiscordRoleService roleService) {
        this.bindingService = bindingService;
        this.messagesConfig = messagesConfig;
        this.roleService = roleService;
    }

    public void handlePrivateMessage(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw().trim();
        String discordId = event.getAuthor().getId();

        UUID existingPlayerUUID = bindingService.getPlayerByBinding(BindingType.DISCORD, discordId);
        if (existingPlayerUUID != null) {
            String playerName = resolvePlayerName(existingPlayerUUID);
            sendDiscordMessage(event, messagesConfig.getDiscordAlreadyBound(playerName));
            return;
        }

        if (CODE_PATTERN.matcher(message).matches()) {
            handleVerificationCode(event, message, discordId);
        } else {
            sendDiscordMessage(event, messagesConfig.getDiscordInstructions());
        }
    }

    private void handleVerificationCode(MessageReceivedEvent event, String code, String discordId) {
        RedeemResult result = bindingService.redeemCode(BindingType.DISCORD, discordId, code);
        switch (result.status()) {
            case BOUND -> {
                String playerName = resolvePlayerName(result.playerUUID());
                sendDiscordMessage(event, messagesConfig.getDiscordBindingSuccess(playerName));
                roleService.assignRoleAndNicknameIfEnabled(discordId, result.playerUUID(), playerName);
            }
            case INVALID_CODE -> sendDiscordMessage(event, messagesConfig.getDiscordInvalidCode());
            case LOCKED -> sendDiscordMessage(event, messagesConfig.getDiscordTooManyAttempts(result.lockedMinutes()));
            case ALREADY_BOUND -> sendDiscordMessage(event,
                    messagesConfig.getDiscordCodeUsed(resolvePlayerName(result.playerUUID())));
            case BINDING_FAILED -> sendDiscordMessage(event, messagesConfig.getDiscordBindingError());
        }
    }

    private String resolvePlayerName(UUID playerUUID) {
        String name = bindingService.getPlayerName(playerUUID);
        return name != null && !name.isBlank() ? name : playerUUID.toString();
    }

    private void sendDiscordMessage(MessageReceivedEvent event, String text) {
        if (text == null || text.isEmpty()) return;

        if (Colors.containsHexColors(text)) {
            String hexColor = Colors.extractHexColor(text);
            int colorInt = hexColor != null ? Colors.hexToInt(hexColor) : -1;
            String cleanText = Colors.processDiscordColors(text);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setDescription(cleanText);
            if (colorInt != -1) {
                embed.setColor(new Color(colorInt));
            }
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        } else {
            event.getChannel().sendMessage(text).queue();
        }
    }
}
