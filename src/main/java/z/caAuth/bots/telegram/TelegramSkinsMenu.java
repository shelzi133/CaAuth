package z.caAuth.bots.telegram;

import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import z.caAuth.account.BindingType;
import z.caAuth.account.SkinProfile;
import z.caAuth.account.SkinSource;
import z.caAuth.binding.BindingService;
import z.caAuth.config.SkinsBotMessages;
import z.caAuth.config.SkinsConfig;
import z.caAuth.skins.SkinService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TelegramSkinsMenu {

    public static final String CALLBACK_PREFIX = "skin_";
    public static final String MENU_ACTION = "skins";

    private static final String ACTION_OPEN = "open";
    private static final String ACTION_ACCOUNT = "acc_";
    private static final String ACTION_VIEW = "view_";
    private static final String ACTION_EDIT = "edit_";
    private static final String ACTION_TYPE = "type_";
    private static final String ACTION_DELETE = "del_";
    private static final String ACTION_DELETE_YES = "delyes_";
    private static final String ACTION_DELETE_NO = "delno_";
    private static final String KIND_NICK = "nick";
    private static final String KIND_URL = "url";
    private static final String KIND_PNG = "png";
    private static final String MENU_BACK = "menu_back";
    private static final String MENU_OPEN = "menu_open";
    private static final int MAX_LABEL_LENGTH = 40;
    private static final long MILLIS_PER_SECOND = 1000L;

    private record Awaiting(UUID account, int slot, String kind, long expiresAt) {
    }

    private final BindingService bindingService;
    private final SkinService skins;
    private final SkinsConfig config;
    private final SkinsBotMessages botMessages;
    private final TelegramSender sender;
    private final Map<String, Awaiting> awaiting = new ConcurrentHashMap<>();

    public TelegramSkinsMenu(BindingService bindingService, SkinService skins, SkinsConfig config,
                             SkinsBotMessages botMessages, TelegramSender sender) {
        this.bindingService = bindingService;
        this.skins = skins;
        this.config = config;
        this.botMessages = botMessages;
        this.sender = sender;
    }

    public InlineKeyboardButton menuButton() {
        return callbackButton(botMessages.skins.bot.menuButton, TelegramMenu.CALLBACK_PREFIX + MENU_ACTION);
    }

    public boolean isSkinsCallback(String data) {
        return data != null && data.startsWith(CALLBACK_PREFIX);
    }

    public boolean isAwaitingInput(String telegramId) {
        Awaiting input = awaiting.get(telegramId);
        if (input == null) {
            return false;
        }
        if (input.expiresAt() < System.currentTimeMillis()) {
            awaiting.remove(telegramId);
            return false;
        }
        return true;
    }

    public void cancelInput(String telegramId) {
        awaiting.remove(telegramId);
    }

    public void open(String telegramId, String chatId, Integer messageId) {
        UUID owner = boundPlayer(telegramId, chatId);
        if (owner == null) {
            return;
        }
        if (!skins.isAvailable()) {
            replace(chatId, messageId, botMessages.skins.bot.unavailable, keyboard(row(callbackButton(botMessages.skins.bot.backButton, MENU_BACK))));
            return;
        }
        replace(chatId, messageId, botMessages.skins.bot.accountTitle, accountRows(owner));
    }

    public void handleCallback(String data, String telegramId, String chatId, Integer messageId) {
        String action = data.substring(CALLBACK_PREFIX.length());
        if (!action.startsWith(ACTION_TYPE)) {
            awaiting.remove(telegramId);
        }
        if (action.equals(ACTION_OPEN)) {
            open(telegramId, chatId, messageId);
            return;
        }
        UUID owner = boundPlayer(telegramId, chatId);
        if (owner == null) {
            return;
        }
        if (action.startsWith(ACTION_ACCOUNT)) {
            SkinService.Account account = skins.account(owner, action.substring(ACTION_ACCOUNT.length()));
            if (account == null) {
                sendResult(chatId, botMessages.skins.bot.accountNotFound, null);
                return;
            }
            replace(chatId, messageId, botMessages.skins.bot.hubTitle.replace("{account}", account.name()), hubRows(account));
        } else if (action.startsWith(ACTION_VIEW)) {
            withSlot(owner, chatId, action.substring(ACTION_VIEW.length()),
                    (account, slot) -> view(chatId, messageId, account, slot));
        } else if (action.startsWith(ACTION_EDIT)) {
            withSlot(owner, chatId, action.substring(ACTION_EDIT.length()),
                    (account, slot) -> chooseType(chatId, messageId, account, slot));
        } else if (action.startsWith(ACTION_TYPE)) {
            String rest = action.substring(ACTION_TYPE.length());
            int split = rest.lastIndexOf('_');
            if (split > 0) {
                String kind = rest.substring(split + 1);
                withSlot(owner, chatId, rest.substring(0, split),
                        (account, slot) -> chooseSource(telegramId, chatId, messageId, account, slot, kind));
            }
        } else if (action.startsWith(ACTION_DELETE_YES)) {
            withSlot(owner, chatId, action.substring(ACTION_DELETE_YES.length()),
                    (account, slot) -> confirmDelete(chatId, messageId, account, slot));
        } else if (action.startsWith(ACTION_DELETE_NO)) {
            withSlot(owner, chatId, action.substring(ACTION_DELETE_NO.length()),
                    (account, slot) -> view(chatId, messageId, account, slot));
        } else if (action.startsWith(ACTION_DELETE)) {
            withSlot(owner, chatId, action.substring(ACTION_DELETE.length()),
                    (account, slot) -> askDelete(chatId, messageId, account, slot));
        }
    }

    public void requestPng(String telegramId, UUID account, int slot) {
        long expiresAt = System.currentTimeMillis() + config.inputTimeoutSeconds * MILLIS_PER_SECOND;
        awaiting.put(telegramId, new Awaiting(account, slot, KIND_PNG, expiresAt));
        sendHtml(telegramId, botMessages.skins.bot.pngPrompt
                .replace("{slot}", String.valueOf(slot))
                .replace("{seconds}", String.valueOf(config.inputTimeoutSeconds)));
    }

    public void handleText(String telegramId, String chatId, String text) {
        Awaiting input = awaiting.remove(telegramId);
        if (input == null) {
            return;
        }
        if (input.expiresAt() < System.currentTimeMillis()) {
            sendResult(chatId, botMessages.skins.bot.expired, null);
            return;
        }
        if (input.kind().equals(KIND_PNG)) {
            awaiting.put(telegramId, input);
            sendHtml(chatId, botMessages.skins.bot.pngWaitHint);
            return;
        }
        boolean nick = input.kind().equals(KIND_NICK);
        CompletableFuture
                .supplyAsync(() -> nick
                        ? skins.setFromNick(input.account(), input.slot(), text)
                        : skins.setFromUrl(input.account(), input.slot(), text))
                .thenAccept(result -> sendSaveResult(chatId, result.status(), input.account(), input.slot(), input.kind(), text));
    }

    public void handleDocument(String telegramId, String chatId, Document document) {
        if (!isAwaitingInput(telegramId)) {
            return;
        }
        Awaiting input = awaiting.get(telegramId);
        if (!input.kind().equals(KIND_PNG)) {
            sendHtml(chatId, botMessages.skins.bot.pngWaitHint);
            return;
        }
        awaiting.remove(telegramId);
        if (document.getFileSize() != null && document.getFileSize() > config.maxPngBytes) {
            sendSaveResult(chatId, SkinService.Status.TOO_LARGE, input.account(), input.slot(), KIND_PNG, null);
            return;
        }
        String fileId = document.getFileId();
        CompletableFuture
                .supplyAsync(() -> download(fileId))
                .thenApply(bytes -> bytes == null
                        ? new SkinService.Result(SkinService.Status.ERROR, null)
                        : skins.setFromPng(input.account(), input.slot(), bytes))
                .thenAccept(result -> sendSaveResult(chatId, result.status(), input.account(), input.slot(), KIND_PNG, null));
    }

    private void view(String chatId, Integer messageId, UUID account, int slot) {
        SkinProfile profile = skins.profile(account, slot);
        String title = botMessages.skins.bot.profileTitle.replace("{slot}", String.valueOf(slot));
        sender.clearKeyboard(chatId, messageId);
        if (profile == null) {
            sendKeyboard(chatId, title + "\n\n" + botMessages.skins.bot.profileEmpty,
                    keyboard(row(callbackButton(botMessages.skins.bot.setButton, targetAction(ACTION_EDIT, account, slot))),
                            row(callbackButton(botMessages.skins.bot.backButton, accountAction(account)))));
            return;
        }
        String caption = title + "\n\n" + botMessages.skins.bot.profileFilled
                .replace("{input}", TelegramTextFormatter.escapeHtml(profile.input()))
                .replace("{source}", sourceLabel(profile));
        List<List<InlineKeyboardButton>> keyboard = keyboard(
                row(callbackButton(botMessages.skins.bot.editButton, targetAction(ACTION_EDIT, account, slot)),
                        callbackButton(botMessages.skins.bot.deleteButton, targetAction(ACTION_DELETE, account, slot))),
                row(callbackButton(botMessages.skins.bot.backButton, accountAction(account))));
        String texture = profile.textureUrl();
        if (texture != null) {
            sender.sendPhoto(chatId, texture, TelegramTextFormatter.convertMarkdownToHtml(caption), markup(keyboard));
        } else {
            sendKeyboard(chatId, caption, keyboard);
        }
    }

    private void chooseType(String chatId, Integer messageId, UUID account, int slot) {
        replace(chatId, messageId, botMessages.skins.bot.typeTitle.replace("{slot}", String.valueOf(slot)),
                keyboard(
                        row(callbackButton(botMessages.skins.bot.typeNickButton, targetAction(ACTION_TYPE, account, slot) + "_" + KIND_NICK)),
                        row(callbackButton(botMessages.skins.bot.typeUrlButton, targetAction(ACTION_TYPE, account, slot) + "_" + KIND_URL)),
                        row(callbackButton(botMessages.skins.bot.typePngButton, targetAction(ACTION_TYPE, account, slot) + "_" + KIND_PNG)),
                        row(callbackButton(botMessages.skins.bot.backButton, targetAction(ACTION_VIEW, account, slot)))));
    }

    private void chooseSource(String telegramId, String chatId, Integer messageId, UUID account, int slot, String kind) {
        long expiresAt = System.currentTimeMillis() + config.inputTimeoutSeconds * MILLIS_PER_SECOND;
        awaiting.put(telegramId, new Awaiting(account, slot, kind, expiresAt));
        String prompt = switch (kind) {
            case KIND_NICK -> botMessages.skins.bot.telegramNickPrompt;
            case KIND_URL -> botMessages.skins.bot.telegramUrlPrompt;
            default -> botMessages.skins.bot.pngPrompt;
        };
        replace(chatId, messageId, prompt
                        .replace("{slot}", String.valueOf(slot))
                        .replace("{seconds}", String.valueOf(config.inputTimeoutSeconds)),
                keyboard(row(callbackButton(botMessages.skins.bot.backButton, targetAction(ACTION_VIEW, account, slot)))));
    }

    private void askDelete(String chatId, Integer messageId, UUID account, int slot) {
        replace(chatId, messageId, botMessages.skins.bot.deleteConfirm.replace("{slot}", String.valueOf(slot)),
                keyboard(row(callbackButton(botMessages.skins.bot.deleteYesButton, targetAction(ACTION_DELETE_YES, account, slot)),
                        callbackButton(botMessages.skins.bot.deleteNoButton, targetAction(ACTION_DELETE_NO, account, slot)))));
    }

    private void confirmDelete(String chatId, Integer messageId, UUID account, int slot) {
        sender.clearKeyboard(chatId, messageId);
        skins.remove(account, slot);
        sendKeyboard(chatId, botMessages.skins.bot.deleted.replace("{slot}", String.valueOf(slot)), resultKeyboard(account));
    }

    private void sendSaveResult(String chatId, SkinService.Status status, UUID account, int slot, String kind, String value) {
        String text = switch (status) {
            case OK -> botMessages.skins.bot.saved.replace("{slot}", String.valueOf(slot));
            case INVALID_INPUT -> kind.equals(KIND_NICK) ? botMessages.skins.bot.invalidNick : botMessages.skins.bot.invalidUrl;
            case NOT_FOUND -> botMessages.skins.bot.notFound.replace("{input}", TelegramTextFormatter.escapeHtml(value));
            case UNAVAILABLE -> botMessages.skins.bot.unavailable;
            case INVALID_PNG -> botMessages.skins.bot.invalidPng;
            case TOO_LARGE -> botMessages.skins.bot.tooLarge.replace("{max}", String.valueOf(config.maxPngBytes / 1024));
            case ERROR -> botMessages.skins.bot.saveError;
        };
        sendResult(chatId, text, account);
    }

    private void sendResult(String chatId, String text, UUID account) {
        sendKeyboard(chatId, text, resultKeyboard(account));
    }

    private List<List<InlineKeyboardButton>> resultKeyboard(UUID account) {
        String hubAction = account != null ? accountAction(account) : CALLBACK_PREFIX + ACTION_OPEN;
        return keyboard(row(callbackButton(botMessages.skins.bot.hubButton, hubAction),
                callbackButton(botMessages.skins.bot.openMenuButton, MENU_OPEN)));
    }

    private List<List<InlineKeyboardButton>> accountRows(UUID owner) {
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (SkinService.Account account : skins.accounts(owner)) {
            buttons.add(callbackButton(shorten(account.name()), accountAction(account.uuid())));
        }
        List<List<InlineKeyboardButton>> rows = chunk(buttons, botMessages.skins.layout.telegramSkinsAccountsPerRow);
        rows.add(row(callbackButton(botMessages.skins.bot.backButton, MENU_BACK)));
        return rows;
    }

    private List<List<InlineKeyboardButton>> hubRows(SkinService.Account account) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> current = new ArrayList<>();
        for (int slot = 1; slot <= profileCount(); slot++) {
            SkinProfile profile = skins.profile(account.uuid(), slot);
            String name = profile == null ? config.labels.emptyName : sourceName(profile);
            String label = botMessages.skins.bot.hubProfileButton
                    .replace("{slot}", String.valueOf(slot))
                    .replace("{name}", shorten(name));
            current.add(callbackButton(label, targetAction(ACTION_VIEW, account.uuid(), slot)));
            if (current.size() == botMessages.skins.layout.telegramSkinsProfilesPerRow) {
                rows.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            rows.add(current);
        }
        rows.add(row(callbackButton(botMessages.skins.bot.backButton, CALLBACK_PREFIX + ACTION_OPEN)));
        return rows;
    }

    private void withSlot(UUID owner, String chatId, String target, SlotAction action) {
        int split = target.lastIndexOf('_');
        SkinService.Account account = split > 0 ? skins.account(owner, target.substring(0, split)) : null;
        if (account == null) {
            sendResult(chatId, botMessages.skins.bot.accountNotFound, null);
            return;
        }
        int slot = parseSlot(target.substring(split + 1));
        if (slot < 1 || slot > profileCount()) {
            sendResult(chatId, botMessages.skins.bot.notFound.replace("{input}", target), account.uuid());
            return;
        }
        action.run(account.uuid(), slot);
    }

    private UUID boundPlayer(String telegramId, String chatId) {
        UUID owner = bindingService.getPlayerByBinding(BindingType.TELEGRAM, telegramId);
        if (owner == null) {
            sendHtml(chatId, botMessages.skins.bot.notBound);
        }
        return owner;
    }

    private byte[] download(String fileId) {
        try {
            return sender.fetchFile(fileId);
        } catch (Exception e) {
            return null;
        }
    }

    private String accountAction(UUID account) {
        return CALLBACK_PREFIX + ACTION_ACCOUNT + account;
    }

    private String targetAction(String action, UUID account, int slot) {
        return CALLBACK_PREFIX + action + account + "_" + slot;
    }

    private String sourceName(SkinProfile profile) {
        return profile.source() == SkinSource.PNG ? config.labels.sourcePng : profile.input();
    }

    private String sourceLabel(SkinProfile profile) {
        return switch (profile.source()) {
            case NICK -> config.labels.sourceNick;
            case URL -> config.labels.sourceUrl;
            case PNG -> config.labels.sourcePng;
        };
    }

    private String shorten(String text) {
        return text.length() <= MAX_LABEL_LENGTH ? text : text.substring(0, MAX_LABEL_LENGTH - 1) + "…";
    }

    private int profileCount() {
        return config.gui.profileSlots.size();
    }

    private static int parseSlot(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void replace(String chatId, Integer messageId, String rawText, List<List<InlineKeyboardButton>> rows) {
        sender.clearKeyboard(chatId, messageId);
        sendKeyboard(chatId, rawText, rows);
    }

    private void sendKeyboard(String chatId, String rawText, List<List<InlineKeyboardButton>> rows) {
        sender.sendMessage(chatId, TelegramTextFormatter.convertMarkdownToHtml(rawText), true, markup(rows));
    }

    private void sendHtml(String chatId, String rawText) {
        sender.sendMessage(chatId, TelegramTextFormatter.convertMarkdownToHtml(rawText), true);
    }

    private static InlineKeyboardMarkup markup(List<List<InlineKeyboardButton>> rows) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    private static InlineKeyboardButton callbackButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private static List<List<InlineKeyboardButton>> chunk(List<InlineKeyboardButton> buttons, int perRow) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += perRow) {
            rows.add(new ArrayList<>(buttons.subList(i, Math.min(i + perRow, buttons.size()))));
        }
        return rows;
    }

    private static List<InlineKeyboardButton> row(InlineKeyboardButton... buttons) {
        return new ArrayList<>(List.of(buttons));
    }

    @SafeVarargs
    private static List<List<InlineKeyboardButton>> keyboard(List<InlineKeyboardButton>... rows) {
        return new ArrayList<>(List.of(rows));
    }

    @FunctionalInterface
    private interface SlotAction {
        void run(UUID account, int slot);
    }
}
