package z.caAuth.bots.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface TelegramSender {
    boolean isRunning();
    void sendMessage(String chatId, String text, boolean useHtml);
    void sendMessage(String chatId, String text, boolean useHtml, InlineKeyboardMarkup keyboard);
}