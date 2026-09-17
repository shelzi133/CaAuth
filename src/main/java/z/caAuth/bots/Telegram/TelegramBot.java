package z.caAuth.bots.Telegram;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

import z.caAuth.confirmation.LoginConfirmationManager;
import z.caAuth.integration.BotIntegration;
import z.caAuth.util.ColorManager;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Telegram бот для привязки аккаунтов и подтверждения входа на сервер
 */
public class TelegramBot extends TelegramLongPollingBot {
    private final JavaPlugin plugin;
    private final BotIntegration botIntegration;
    private final TelegramBotMessages messagesConfig;
    private final LoginConfirmationManager confirmationManager;
    private final String token;
    private boolean isRunning = false;

    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");
    private static final String LOGIN_CONFIRM_PREFIX = "login_confirm_";
    private static final String LOGIN_DENY_PREFIX = "login_deny_";

    public TelegramBot(JavaPlugin plugin, BotIntegration botIntegration, TelegramBotMessages messagesConfig,
                        LoginConfirmationManager confirmationManager, String token) {
        this(plugin, botIntegration, messagesConfig, confirmationManager, token, new DefaultBotOptions());
    }

    public TelegramBot(JavaPlugin plugin, BotIntegration botIntegration, TelegramBotMessages messagesConfig,
                        LoginConfirmationManager confirmationManager, String token, DefaultBotOptions options) {
        super(options);
        this.plugin = plugin;
        this.botIntegration = botIntegration;
        this.messagesConfig = messagesConfig;
        this.confirmationManager = confirmationManager;
        this.token = token;
    }

    @Override
    public String getBotUsername() {
        return "CaAuthBot";
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText().trim();
            String telegramId = String.valueOf(message.getFrom().getId());
            String chatId = String.valueOf(message.getChatId());
            if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
                return;
            }

            handleMessage(text, telegramId, chatId);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String chatId = String.valueOf(callbackQuery.getMessage().getChatId());

        if (data.startsWith(LOGIN_CONFIRM_PREFIX)) {
            String loginToken = data.substring(LOGIN_CONFIRM_PREFIX.length());
            confirmationManager.confirm(loginToken);
            answerCallbackQuery(callbackQuery.getId(), "✅ Вход подтверждён");
            editToStatusText(chatId, messageId, "✅ Вход подтверждён.");
            return;
        }
        if (data.startsWith(LOGIN_DENY_PREFIX)) {
            String loginToken = data.substring(LOGIN_DENY_PREFIX.length());
            confirmationManager.deny(loginToken);
            answerCallbackQuery(callbackQuery.getId(), "❌ Вход отклонён");
            editToStatusText(chatId, messageId, "❌ Вход отклонён, игрок кикнут с сервера.");
            return;
        }
        answerCallbackQuery(callbackQuery.getId(), "❓ Неизвестное действие");
    }

    private void answerCallbackQuery(String callbackId, String text) {
        try {
            org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery answer =
                    new org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackId);
            answer.setText(text);
            answer.setShowAlert(false);
            execute(answer);
        } catch (TelegramApiException e) {
            plugin.getLogger().warning("Ошибка при ответе на CallbackQuery: " + e.getMessage());
        }
    }

    private void editToStatusText(String chatId, Integer messageId, String text) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(messageId);
        editMessage.setText(text);
        editMessage.setParseMode("HTML");
        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            plugin.getLogger().warning("Ошибка при редактировании сообщения: " + e.getMessage());
        }
    }

    /**
     * Отправляет пользователю Telegram запрос на подтверждение входа на сервер с кнопками
     * "Подтвердить" / "Это не я".
     *
     * @return true, если запрос на отправку был принят
     */
    public boolean sendLoginConfirmation(String telegramId, String playerName, String loginToken, int timeoutSeconds) {
        if (!isRunning() || telegramId == null || telegramId.isBlank()) {
            return false;
        }
        try {
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
            confirmBtn.setText("✅ Подтвердить вход");
            confirmBtn.setCallbackData(LOGIN_CONFIRM_PREFIX + loginToken);
            row.add(confirmBtn);

            InlineKeyboardButton denyBtn = new InlineKeyboardButton();
            denyBtn.setText("❌ Это не я");
            denyBtn.setCallbackData(LOGIN_DENY_PREFIX + loginToken);
            row.add(denyBtn);

            rows.add(row);
            keyboard.setKeyboard(rows);

            String text = "<b>🔐 Подтверждение входа на сервер</b>\n\n" +
                    "Игрок <b>" + escapeHtml(playerName) + "</b> заходит на сервер.\n" +
                    "Если это вы - подтвердите вход в течение <b>" + timeoutSeconds + " секунд</b>, иначе вы будете кикнуты.";

            sendMessage(telegramId, text, true, keyboard);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при отправке запроса подтверждения входа в Telegram: " + e.getMessage());
            return false;
        }
    }

    private void handleMessage(String text, String telegramId, String chatId) {
        UUID existingPlayerUUID = botIntegration.getPlayerByTelegramId(telegramId);
        if (existingPlayerUUID != null) {
            String playerName = botIntegration.getPlayerName(existingPlayerUUID);
            String message = messagesConfig.getTelegramAlreadyBound(playerName);
            sendMessage(chatId, convertMarkdownToHtml(message), true);
            return;
        }
        if (text.equalsIgnoreCase("/start") || text.equalsIgnoreCase("/help") || text.equalsIgnoreCase("start")) {
            sendInstructions(chatId);
            return;
        }
        if (CODE_PATTERN.matcher(text).matches()) {
            handleVerificationCode(text, telegramId, chatId);
            return;
        }
        if (!text.equalsIgnoreCase("/start") && !text.equalsIgnoreCase("/help") && !text.equalsIgnoreCase("start")) {
            handlePlayerNameBinding(text, telegramId, chatId);
            return;
        }
    }

    private void handleVerificationCode(String code, String telegramId, String chatId) {
        boolean found = false;

        for (org.bukkit.OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            UUID playerUUID = offlinePlayer.getUniqueId();
            String pendingCode = botIntegration.getAccountStorage().getPendingCode(playerUUID);
            if (pendingCode != null && pendingCode.equals(code)) {
                if (botIntegration.isAccountBound(playerUUID)) {
                    String message = messagesConfig.getTelegramCodeUsed(offlinePlayer.getName());
                    message = convertMarkdownToHtml(message);
                    sendMessage(chatId, message, true);
                    return;
                }

                if (botIntegration.bindTelegramAccount(playerUUID, telegramId)) {
                    String message = messagesConfig.getTelegramBindingSuccess(offlinePlayer.getName());
                    message = convertMarkdownToHtml(message);
                    sendMessage(chatId, message, true);
                    found = true;
                    break;
                } else {
                    sendMessage(chatId, messagesConfig.getTelegramBindingError(), false);
                    return;
                }
            }
        }

        if (!found) {
            sendMessage(chatId, messagesConfig.getTelegramInvalidCode(), false);
        }
    }

    private void handlePlayerNameBinding(String playerName, String telegramId, String chatId) {
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerName);

        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            sendInstructions(chatId);
            return;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();
        if (botIntegration.isAccountBound(playerUUID)) {
            String message = messagesConfig.getTelegramCodeUsed(offlinePlayer.getName());
            message = convertMarkdownToHtml(message);
            sendMessage(chatId, message, true);
            return;
        }

        String pendingCode = botIntegration.getAccountStorage().getPendingCode(playerUUID);
        if (pendingCode == null || pendingCode.isEmpty()) {
            sendMessage(chatId, "❌ У игрока <b>" + playerName + "</b> нет активного кода верификации. Попросите игрока выполнить команду <code>/binding code</code> в игре.", true);
            return;
        }
        if (botIntegration.bindTelegramAccount(playerUUID, telegramId)) {
            String message = messagesConfig.getTelegramBindingSuccess(offlinePlayer.getName());
            message = convertMarkdownToHtml(message);
            sendMessage(chatId, message, true);
        } else {
            sendMessage(chatId, messagesConfig.getTelegramBindingError(), false);
        }
    }

    private void sendInstructions(String chatId) {
        String instructions = messagesConfig.getTelegramInstructions();
        instructions = convertMarkdownToHtml(instructions);
        sendMessage(chatId, instructions, true);
    }

    private String convertMarkdownToHtml(String text) {
        if (text == null) {
            return "";
        }
        text = ColorManager.processTelegramColors(text);
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        text = text.replaceAll("```py\\s*(.+?)\\s*```", "<code>$1</code>");
        text = text.replaceAll("```(.+?)```", "<code>$1</code>");
        return text;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void sendMessage(String chatId, String text) {
        sendMessage(chatId, text, false, null);
    }

    private void sendMessage(String chatId, String text, boolean useHtml) {
        sendMessage(chatId, text, useHtml, null);
    }

    public void start() {
        if (token == null || token.isEmpty()) {
            plugin.getLogger().warning("Telegram токен не настроен!");
            return;
        }

        try {
            isRunning = true;
            plugin.getLogger().info("Telegram бот успешно запущен!");
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при запуске Telegram бота: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Останавливает бота. Флаг isRunning сбрасывается, чтобы sendMessage/sendLoginConfirmation
     * не пытались слать запросы после остановки плагина/at /reload.
     * Саму сессию (BotSession, поток ReaderThread) останавливает вызывающий код
     * через botSession.stop() — см. CaAuth.stopBots().
     */
    public void stop() {
        isRunning = false;
    }

    private void sendMessage(String chatId, String text, boolean useHtml, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        if (useHtml) {
            message.setParseMode("HTML");
        }

        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().warning("Ошибка при отправке сообщения в Telegram: " + e.getMessage());
        }
    }

    public void sendMessageToUser(String telegramId, String message) {
        if (!isRunning()) {
            return;
        }

        try {
            sendMessage(telegramId, message, true);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при отправке сообщения в Telegram: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public String resolveChatUserDisplay(String telegramUserId) {
        if (!isRunning() || telegramUserId == null || telegramUserId.isBlank()) {
            return null;
        }
        try {
            long chatId = Long.parseLong(telegramUserId.trim());
            org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat getChat =
                    new org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat();
            getChat.setChatId(chatId);
            org.telegram.telegrambots.meta.api.objects.Chat chat = execute(getChat);
            if (chat.getUserName() != null && !chat.getUserName().isBlank()) {
                return "@" + chat.getUserName();
            }
            String fn = chat.getFirstName();
            if (fn != null && !fn.isBlank()) {
                String ln = chat.getLastName();
                if (ln != null && !ln.isBlank()) {
                    return fn + " " + ln;
                }
                return fn;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
