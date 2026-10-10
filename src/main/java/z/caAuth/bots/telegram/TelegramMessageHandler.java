package z.caAuth.bots.telegram;

import z.caAuth.account.BindingType;
import z.caAuth.binding.BindingService;
import z.caAuth.binding.BindingService.RedeemResult;
import z.caAuth.config.TelegramBotMessages;

import java.util.UUID;
import java.util.regex.Pattern;

public class TelegramMessageHandler {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    private final BindingService bindingService;
    private final TelegramBotMessages messagesConfig;
    private final TelegramSender sender;

    public TelegramMessageHandler(BindingService bindingService, TelegramBotMessages messagesConfig,
                                   TelegramSender sender) {
        this.bindingService = bindingService;
        this.messagesConfig = messagesConfig;
        this.sender = sender;
    }

    public void handleMessage(String text, String telegramId, String chatId) {
        UUID existingPlayerUUID = bindingService.getPlayerByBinding(BindingType.TELEGRAM, telegramId);
        if (existingPlayerUUID != null) {
            String playerName = bindingService.getPlayerName(existingPlayerUUID);
            sendHtml(chatId, messagesConfig.getTelegramAlreadyBound(playerName));
            return;
        }

        if (CODE_PATTERN.matcher(text).matches()) {
            handleVerificationCode(text, telegramId, chatId);
        } else {
            sendHtml(chatId, messagesConfig.getTelegramInstructions());
        }
    }

    private void handleVerificationCode(String code, String telegramId, String chatId) {
        RedeemResult result = bindingService.redeemCode(BindingType.TELEGRAM, telegramId, code);
        switch (result.status()) {
            case BOUND -> sendHtml(chatId, messagesConfig.getTelegramBindingSuccess(result.playerName()));
            case INVALID_CODE -> sender.sendMessage(chatId, messagesConfig.getTelegramInvalidCode(), false);
            case LOCKED -> sendHtml(chatId, messagesConfig.getTelegramTooManyAttempts(result.lockedMinutes()));
            case ALREADY_BOUND -> sendHtml(chatId, messagesConfig.getTelegramCodeUsed(result.playerName()));
            case BINDING_FAILED -> sender.sendMessage(chatId, messagesConfig.getTelegramBindingError(), false);
        }
    }

    private void sendHtml(String chatId, String rawMessage) {
        sender.sendMessage(chatId, TelegramTextFormatter.convertMarkdownToHtml(rawMessage), true);
    }
}
