package z.caAuth.bots.telegram;

public interface TelegramSender {
    boolean isRunning();
    void sendMessage(String chatId, String text, boolean useHtml);
}
