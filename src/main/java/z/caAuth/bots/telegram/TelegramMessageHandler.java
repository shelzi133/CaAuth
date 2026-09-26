package z.caAuth.bots.telegram;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import z.caAuth.auth.integration.BotIntegration;

import java.util.UUID;
import java.util.regex.Pattern;

public class TelegramMessageHandler {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    private final BotIntegration botIntegration;
    private final TelegramBotMessages messagesConfig;
    private final TelegramSender sender;

    public TelegramMessageHandler(BotIntegration botIntegration, TelegramBotMessages messagesConfig,
                                   TelegramSender sender) {
        this.botIntegration = botIntegration;
        this.messagesConfig = messagesConfig;
        this.sender = sender;
    }

    public void handleMessage(String text, String telegramId, String chatId) {
        UUID existingPlayerUUID = botIntegration.getPlayerByTelegramId(telegramId);
        if (existingPlayerUUID != null) {
            String playerName = botIntegration.getPlayerName(existingPlayerUUID);
            sendHtml(chatId, messagesConfig.getTelegramAlreadyBound(playerName));
            return;
        }

        boolean isCommand = text.equalsIgnoreCase("/start")
                || text.equalsIgnoreCase("/help")
                || text.equalsIgnoreCase("start");

        if (isCommand) {
            sendInstructions(chatId);
            return;
        }

        if (CODE_PATTERN.matcher(text).matches()) {
            handleVerificationCode(text, telegramId, chatId);
            return;
        }

        handlePlayerNameBinding(text, telegramId, chatId);
    }

    private void handleVerificationCode(String code, String telegramId, String chatId) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            UUID playerUUID = offlinePlayer.getUniqueId();
            String pendingCode = botIntegration.getAccountStorage().getPendingCode(playerUUID);
            if (pendingCode == null || !pendingCode.equals(code)) continue;

            if (botIntegration.isAccountBound(playerUUID)) {
                sendHtml(chatId, messagesConfig.getTelegramCodeUsed(offlinePlayer.getName()));
            } else {
                bindAndNotify(chatId, playerUUID, offlinePlayer.getName(), telegramId);
            }
            return;
        }

        sender.sendMessage(chatId, messagesConfig.getTelegramInvalidCode(), false);
    }

    private void handlePlayerNameBinding(String playerName, String telegramId, String chatId) {
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            sendInstructions(chatId);
            return;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();
        if (botIntegration.isAccountBound(playerUUID)) {
            sendHtml(chatId, messagesConfig.getTelegramCodeUsed(offlinePlayer.getName()));
            return;
        }

        String pendingCode = botIntegration.getAccountStorage().getPendingCode(playerUUID);
        if (pendingCode == null || pendingCode.isEmpty()) {
            sender.sendMessage(chatId, "❌ У игрока <b>" + playerName + "</b> нет активного кода верификации. Попросите игрока выполнить команду <code>/binding code</code> в игре.", true);
            return;
        }

        bindAndNotify(chatId, playerUUID, offlinePlayer.getName(), telegramId);
    }

    private void bindAndNotify(String chatId, UUID playerUUID, String playerName, String telegramId) {
        if (!botIntegration.bindTelegramAccount(playerUUID, telegramId)) {
            sender.sendMessage(chatId, messagesConfig.getTelegramBindingError(), false);
            return;
        }
        sendHtml(chatId, messagesConfig.getTelegramBindingSuccess(playerName));
    }

    private void sendInstructions(String chatId) {
        sendHtml(chatId, messagesConfig.getTelegramInstructions());
    }

    private void sendHtml(String chatId, String rawMessage) {
        sender.sendMessage(chatId, TelegramTextFormatter.convertMarkdownToHtml(rawMessage), true);
    }
}
