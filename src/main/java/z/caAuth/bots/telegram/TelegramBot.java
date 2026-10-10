package z.caAuth.bots.telegram;

import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import z.caAuth.account.BindingType;
import z.caAuth.binding.BindingService;
import z.caAuth.bots.ChatBot;
import z.caAuth.config.TelegramBotMessages;
import z.caAuth.login.LoginConfirmationManager;

import java.util.ArrayList;
import java.util.List;

public class TelegramBot extends TelegramLongPollingBot implements TelegramSender, ChatBot {

    private static final String LOGIN_CONFIRM_PREFIX = "login_confirm_";
    private static final String LOGIN_DENY_PREFIX = "login_deny_";

    private final JavaPlugin plugin;
    private final LoginConfirmationManager confirmationManager;
    private final TelegramMessageHandler messageHandler;
    private final TelegramMenu menu;
    private final TelegramBotMessages messages;
    private final String token;

    private boolean isRunning = false;

    public TelegramBot(JavaPlugin plugin, BindingService bindingService, TelegramBotMessages messagesConfig,
                       LoginConfirmationManager confirmationManager, String token, DefaultBotOptions options) {
        super(options);
        this.plugin = plugin;
        this.confirmationManager = confirmationManager;
        this.token = token;
        this.messages = messagesConfig;
        this.messageHandler = new TelegramMessageHandler(bindingService, messagesConfig, this);
        this.menu = new TelegramMenu(bindingService, messagesConfig, this);
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
            if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
                return;
            }
            String text = message.getText().trim();
            String telegramId = String.valueOf(message.getFrom().getId());
            String chatId = String.valueOf(message.getChatId());

            if (menu.isMenuCommand(text)) {
                menu.sendMenu(chatId);
                return;
            }

            messageHandler.handleMessage(text, telegramId, chatId);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String chatId = String.valueOf(callbackQuery.getMessage().getChatId());

        if (data.startsWith(LOGIN_CONFIRM_PREFIX)) {
            String loginToken = data.substring(LOGIN_CONFIRM_PREFIX.length());
            confirmationManager.confirm(loginToken);
            answerCallbackQuery(callbackQuery.getId(), messages.getLoginConfirmedAlert());
            editToStatusText(chatId, messageId, messages.getLoginConfirmed());
            return;
        }
        if (data.startsWith(LOGIN_DENY_PREFIX)) {
            String loginToken = data.substring(LOGIN_DENY_PREFIX.length());
            confirmationManager.deny(loginToken);
            answerCallbackQuery(callbackQuery.getId(), messages.getLoginDeniedAlert());
            editToStatusText(chatId, messageId, messages.getLoginDenied());
            return;
        }
        if (menu.isMenuCallback(data)) {
            answerCallbackQuery(callbackQuery.getId(), null);
            menu.handleCallback(data, String.valueOf(callbackQuery.getFrom().getId()), chatId);
            return;
        }
        answerCallbackQuery(callbackQuery.getId(), messages.getUnknownAction());
    }

    private void answerCallbackQuery(String callbackId, String text) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackId);
            if (text != null) {
                answer.setText(text);
            }
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

    @Override
    public boolean sendLoginConfirmation(String telegramId, String playerName, String loginToken, int timeoutSeconds) {
        if (!isRunning() || telegramId == null || telegramId.isBlank()) {
            return false;
        }
        try {
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
            confirmBtn.setText(messages.getLoginConfirmButton());
            confirmBtn.setCallbackData(LOGIN_CONFIRM_PREFIX + loginToken);
            row.add(confirmBtn);

            InlineKeyboardButton denyBtn = new InlineKeyboardButton();
            denyBtn.setText(messages.getLoginDenyButton());
            denyBtn.setCallbackData(LOGIN_DENY_PREFIX + loginToken);
            row.add(denyBtn);

            rows.add(row);
            keyboard.setKeyboard(rows);

            String text = TelegramTextFormatter.convertMarkdownToHtml(messages.getLoginConfirmText(playerName, timeoutSeconds));

            sendMessage(telegramId, text, true, keyboard);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при отправке запроса подтверждения входа в Telegram: " + e.getMessage());
            return false;
        }
    }

    @Override
    public BindingType type() {
        return BindingType.TELEGRAM;
    }

    @Override
    public void start() {
        if (token == null || token.isEmpty()) {
            plugin.getLogger().warning("Telegram токен не настроен!");
            return;
        }
        try {
            isRunning = true;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::registerCommands);
            plugin.getLogger().info("Telegram бот успешно запущен!");
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при запуске Telegram бота: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerCommands() {
        try {
            List<BotCommand> commands = new ArrayList<>();
            commands.add(new BotCommand("menu", messages.getCommandMenuDescription()));
            commands.add(new BotCommand("start", messages.getCommandStartDescription()));
            execute(SetMyCommands.builder()
                    .commands(commands)
                    .scope(new BotCommandScopeDefault())
                    .build());
        } catch (TelegramApiException e) {
            plugin.getLogger().warning("Не удалось зарегистрировать команды Telegram бота: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void sendMessage(String chatId, String text, boolean useHtml) {
        sendMessage(chatId, text, useHtml, null);
    }

    @Override
    public void sendMessage(String chatId, String text, boolean useHtml, InlineKeyboardMarkup keyboard) {
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

    @Override
    public String resolveDisplay(String telegramUserId) {
        if (!isRunning() || telegramUserId == null || telegramUserId.isBlank()) {
            return null;
        }
        try {
            long chatId = Long.parseLong(telegramUserId.trim());
            GetChat getChat = new GetChat();
            getChat.setChatId(chatId);
            Chat chat = execute(getChat);
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