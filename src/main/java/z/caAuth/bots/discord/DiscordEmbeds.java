package z.caAuth.bots.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import z.caAuth.config.DiscordBotMessages;
import z.caAuth.config.DiscordBotMessages.ButtonKey;
import z.caAuth.config.DiscordBotMessages.EmbedType;
import z.caAuth.util.Colors;

import java.awt.Color;
import java.util.Locale;

public class DiscordEmbeds {

    private final DiscordBotMessages messages;

    public DiscordEmbeds(DiscordBotMessages messages) {
        this.messages = messages;
    }

    public MessageEmbed embed(EmbedType type, String text) {
        return embed(type, null, text);
    }

    public MessageEmbed embed(EmbedType type, String title, String text) {
        int override = Colors.containsHexColors(text) ? Colors.hexToInt(Colors.extractHexColor(text)) : -1;
        EmbedBuilder builder = new EmbedBuilder()
                .setDescription(Colors.processDiscordColors(text))
                .setColor(new Color(override != -1 ? override : configuredColor(type)));
        if (title != null && !title.isBlank()) {
            builder.setTitle(title);
        }
        return builder.build();
    }

    public Button button(ButtonKey key, String componentId, String label) {
        return Button.of(configuredStyle(key), componentId, label);
    }

    public Button button(String styleName, String componentId, String label) {
        return Button.of(parseStyle(styleName), componentId, label);
    }

    private int configuredColor(EmbedType type) {
        int color = Colors.hexToInt(messages.getEmbedColor(type));
        return color != -1 ? color : defaultColor(type);
    }

    private int defaultColor(EmbedType type) {
        return switch (type) {
            case INFO -> 0x5865F2;
            case SUCCESS -> 0x57F287;
            case ERROR -> 0xED4245;
            case WARNING -> 0xFFA500;
        };
    }

    private ButtonStyle configuredStyle(ButtonKey key) {
        return parseStyle(messages.getButtonStyle(key));
    }

    private ButtonStyle parseStyle(String name) {
        if (name != null) {
            try {
                ButtonStyle style = ButtonStyle.valueOf(name.trim().toUpperCase(Locale.ROOT));
                if (style != ButtonStyle.LINK && style != ButtonStyle.UNKNOWN) {
                    return style;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ButtonStyle.SECONDARY;
    }
}
