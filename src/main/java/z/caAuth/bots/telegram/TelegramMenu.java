package z.caAuth.bots.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import z.caAuth.account.BindingType;
import z.caAuth.binding.BindingService;
import z.caAuth.config.TelegramBotMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TelegramMenu {

    public static final String CALLBACK_PREFIX = "menu_";

    private static final String ACTION_UNBIND = "unbind";
    private static final String ACTION_UNBIND_YES = "unbind_yes";
    private static final String ACTION_UNBIND_NO = "unbind_no";

    private final BindingService bindingService;
    private final TelegramBotMessages messagesConfig;
    private final TelegramSender sender;

    public TelegramMenu(BindingService bindingService, TelegramBotMessages messagesConfig, TelegramSender sender) {
        this.bindingService = bindingService;
        this.messagesConfig = messagesConfig;
        this.sender = sender;
    }

    public boolean isMenuCommand(String text) {
        String lower = text.toLowerCase();
        return lower.equals("/menu") || lower.startsWith("/menu@");
    }

    public boolean isMenuCallback(String data) {
        return data != null && data.startsWith(CALLBACK_PREFIX);
    }

    public void sendMenu(String chatId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row(button(messagesConfig.getMenuUnbindButton(), ACTION_UNBIND)));
        keyboard.setKeyboard(rows);

        sender.sendMessage(chatId, TelegramTextFormatter.convertMarkdownToHtml(messagesConfig.getTelegramMenuTitle()), true, keyboard);
    }

    public void handleCallback(String data, String telegramId, String chatId) {
        String action = data.substring(CALLBACK_PREFIX.length());
        switch (action) {
            case ACTION_UNBIND:
                requestUnbind(telegramId, chatId);
                break;
            case ACTION_UNBIND_YES:
                confirmUnbind(telegramId, chatId);
                break;
            case ACTION_UNBIND_NO:
                sendHtml(chatId, messagesConfig.getTelegramMenuUnbindCancelled());
                break;
            default:
                sendMenu(chatId);
                break;
        }
    }

    private void requestUnbind(String telegramId, String chatId) {
        UUID playerUuid = bindingService.getPlayerByBinding(BindingType.TELEGRAM, telegramId);
        if (playerUuid == null) {
            sendHtml(chatId, messagesConfig.getTelegramMenuNotBound());
            return;
        }
        String playerName = resolveName(playerUuid);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row(
                button(messagesConfig.getMenuUnbindYesButton(), ACTION_UNBIND_YES),
                button(messagesConfig.getMenuUnbindNoButton(), ACTION_UNBIND_NO)
        ));
        keyboard.setKeyboard(rows);

        sender.sendMessage(chatId,
                TelegramTextFormatter.convertMarkdownToHtml(messagesConfig.getTelegramMenuUnbindConfirm(playerName)),
                true, keyboard);
    }

    private void confirmUnbind(String telegramId, String chatId) {
        UUID playerUuid = bindingService.getPlayerByBinding(BindingType.TELEGRAM, telegramId);
        if (playerUuid == null) {
            sendHtml(chatId, messagesConfig.getTelegramMenuNotBound());
            return;
        }
        String playerName = resolveName(playerUuid);

        if (bindingService.unbindAccount(playerUuid)) {
            sendHtml(chatId, messagesConfig.getTelegramMenuUnbindSuccess(playerName));
        } else {
            sendHtml(chatId, messagesConfig.getTelegramMenuUnbindError());
        }
    }

    private String resolveName(UUID playerUuid) {
        String name = bindingService.getPlayerName(playerUuid);
        return name != null && !name.isBlank() ? name : "?";
    }

    private InlineKeyboardButton button(String text, String action) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(CALLBACK_PREFIX + action);
        return button;
    }

    private List<InlineKeyboardButton> row(InlineKeyboardButton... buttons) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (InlineKeyboardButton button : buttons) {
            row.add(button);
        }
        return row;
    }

    private void sendHtml(String chatId, String rawMessage) {
        sender.sendMessage(chatId, TelegramTextFormatter.convertMarkdownToHtml(rawMessage), true);
    }
}