package z.caAuth.bots.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.auth.integration.BotIntegration;
import z.caAuth.auth.util.ColorManager;

import java.awt.Color;
import java.util.UUID;
import java.util.regex.Pattern;


public class DiscordMessageHandler {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    private final JavaPlugin plugin;
    private final BotIntegration botIntegration;
    private final DiscordBotMessages messagesConfig;
    private final DiscordRoleService roleService;

    public DiscordMessageHandler(JavaPlugin plugin, BotIntegration botIntegration,
                                  DiscordBotMessages messagesConfig, DiscordRoleService roleService) {
        this.plugin = plugin;
        this.botIntegration = botIntegration;
        this.messagesConfig = messagesConfig;
        this.roleService = roleService;
    }

    public void handlePrivateMessage(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw().trim();
        String discordId = event.getAuthor().getId();

        UUID existingPlayerUUID = botIntegration.getPlayerByDiscordId(discordId);
        if (existingPlayerUUID != null) {
            String playerName = botIntegration.getPlayerName(existingPlayerUUID);
            sendDiscordMessage(event, messagesConfig.getDiscordAlreadyBound(playerName));
            return;
        }

        if (CODE_PATTERN.matcher(message).matches()) {
            handleVerificationCode(event, message, discordId);
            return;
        }

        boolean isCommand = message.equalsIgnoreCase("/start")
                || message.equalsIgnoreCase("/help")
                || message.equalsIgnoreCase("start");

        if (isCommand) {
            sendInstructions(event);
        } else {
            handlePlayerNameBinding(event, message, discordId);
        }
    }

    private void handleVerificationCode(MessageReceivedEvent event, String code, String discordId) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            UUID playerUUID = offlinePlayer.getUniqueId();
            String pendingCode = botIntegration.getAccountStorage().getPendingCode(playerUUID);
            if (pendingCode == null || !pendingCode.equals(code)) continue;

            if (botIntegration.isAccountBound(playerUUID)) {
                sendDiscordMessage(event, messagesConfig.getDiscordCodeUsed(offlinePlayer.getName()));
            } else {
                bindAndNotify(event, playerUUID, offlinePlayer.getName(), discordId);
            }
            return;
        }

        sendDiscordMessage(event, messagesConfig.getDiscordInvalidCode());
    }

    private void handlePlayerNameBinding(MessageReceivedEvent event, String playerName, String discordId) {
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            sendInstructions(event);
            return;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();
        if (botIntegration.isAccountBound(playerUUID)) {
            sendDiscordMessage(event, messagesConfig.getDiscordCodeUsed(offlinePlayer.getName()));
            return;
        }

        String pendingCode = botIntegration.getAccountStorage().getPendingCode(playerUUID);
        if (pendingCode == null || pendingCode.isEmpty()) {
            sendDiscordMessage(event, "❌ У игрока **" + playerName + "** нет активного кода верификации. " +
                    "Попросите игрока выполнить команду `/binding code` в игре.");
            return;
        }

        bindAndNotify(event, playerUUID, offlinePlayer.getName(), discordId);
    }

    private void bindAndNotify(MessageReceivedEvent event, UUID playerUUID, String playerName, String discordId) {
        if (!botIntegration.bindDiscordAccount(playerUUID, discordId)) {
            sendDiscordMessage(event, messagesConfig.getDiscordBindingError());
            return;
        }
        sendDiscordMessage(event, messagesConfig.getDiscordBindingSuccess(playerName));
        roleService.assignRoleAndNicknameIfEnabled(discordId, playerUUID, playerName);
    }

    private void sendInstructions(MessageReceivedEvent event) {
        sendDiscordMessage(event, messagesConfig.getDiscordInstructions());
    }

    private void sendDiscordMessage(MessageReceivedEvent event, String text) {
        if (text == null || text.isEmpty()) return;

        if (ColorManager.containsHexColors(text)) {
            String hexColor = ColorManager.extractHexColor(text);
            int colorInt = hexColor != null ? ColorManager.hexToInt(hexColor) : -1;
            String cleanText = ColorManager.processDiscordColors(text);

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
