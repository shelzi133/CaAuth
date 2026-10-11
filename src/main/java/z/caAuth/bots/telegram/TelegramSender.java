package z.caAuth.bots.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface TelegramSender {
    boolean isRunning();
    void sendMessage(String chatId, String text, boolean useHtml);
    void sendMessage(String chatId, String text, boolean useHtml, InlineKeyboardMarkup keyboard);
    void sendPhoto(String chatId, String photoUrl, String caption, InlineKeyboardMarkup keyboard);
    void editMessage(String chatId, Integer messageId, String text, boolean useHtml, InlineKeyboardMarkup keyboard);
    void clearKeyboard(String chatId, Integer messageId);
    byte[] fetchFile(String fileId) throws Exception;
}
