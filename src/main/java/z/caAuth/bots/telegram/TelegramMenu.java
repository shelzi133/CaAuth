package z.caAuth.bots.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import z.caAuth.account.BindingType;
import z.caAuth.banking.BankService;
import z.caAuth.binding.BindingService;
import z.caAuth.binding.PasswordService;
import z.caAuth.binding.TwinkService;
import z.caAuth.binding.TwinkService.Slots;
import z.caAuth.config.SkinsBotMessages;
import z.caAuth.config.TelegramBotMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TelegramMenu {

    public static final String CALLBACK_PREFIX = "menu_";

    private static final String ACTION_OPEN = "open";
    private static final String ACTION_MANAGE = "manage";
    private static final String ACTION_BACK = "back";
    private static final String ACTION_BACK_MANAGE = "back_manage";
    private static final String ACTION_UNBIND = "unbind";
    private static final String ACTION_UNBIND_YES = "unbind_yes";
    private static final String ACTION_UNBIND_NO = "unbind_no";
    private static final String ACTION_PASSWORD = "password";
    private static final String ACTION_PASSWORD_YES = "password_yes";
    private static final String ACTION_PASSWORD_NO = "password_no";
    private static final String ACTION_TWINK = "twink";
    private static final String ACTION_TWINK_ADD = "twink_add";
    private static final String ACTION_TWINK_DELETE = "twink_del_";
    private static final String ACTION_TWINK_DELETE_YES = "twink_delyes_";
    private static final String ACTION_TWINK_DELETE_NO = "twink_delno";
    private static final String ACTION_SKINS = TelegramSkinsMenu.MENU_ACTION;
    private static final String ACTION_BANK = "bank";
    private static final String ACTION_BANK_TRANSFER = "bank_transfer";
    private static final String ACTION_BANK_YES = "bank_yes_";
    private static final String ACTION_BANK_NO = "bank_no";

    private static final long NICK_INPUT_MILLIS = 5 * 60_000L;

    private final BindingService bindingService;
    private final PasswordService passwordService;
    private final TwinkService twinkService;
    private final Map<String, Long> awaitingNick = new ConcurrentHashMap<>();
    private final TelegramBotMessages messagesConfig;
    private final TelegramSender sender;
    private final TelegramSkinsMenu skinsMenu;
    private final SkinsBotMessages skinsBot;
    private final BankService bankService;
    private final Map<String, Long> awaitingTransfer = new ConcurrentHashMap<>();

    public TelegramMenu(BindingService bindingService, PasswordService passwordService, TwinkService twinkService,
                        TelegramBotMessages messagesConfig, TelegramSender sender, TelegramSkinsMenu skinsMenu,
                        SkinsBotMessages skinsBot, BankService bankService) {
        this.bankService = bankService;
        this.bindingService = bindingService;
        this.passwordService = passwordService;
        this.twinkService = twinkService;
        this.messagesConfig = messagesConfig;
        this.sender = sender;
        this.skinsMenu = skinsMenu;
        this.skinsBot = skinsBot;
    }

    public boolean isMenuCommand(String text) {
        String lower = text.toLowerCase();
        return lower.equals("/menu") || lower.startsWith("/menu@");
    }

    public boolean isMenuCallback(String data) {
        return data != null && data.startsWith(CALLBACK_PREFIX);
    }

    public void sendMenu(String chatId) {
        sendKeyboard(chatId, messagesConfig.getTelegramMenuTitle(), mainMenuRows().toArray(new List[0]));
    }

    public void handleCallback(String data, String telegramId, String chatId, Integer messageId) {
        String action = data.substring(CALLBACK_PREFIX.length());
        if (!action.equals(ACTION_TWINK_ADD)) {
            awaitingNick.remove(telegramId);
        }
        if (!action.equals(ACTION_BANK_TRANSFER)) {
            awaitingTransfer.remove(telegramId);
        }
        if (action.startsWith(ACTION_BANK_YES)) {
            sender.clearKeyboard(chatId, messageId);
            confirmBankTransfer(telegramId, chatId, action.substring(ACTION_BANK_YES.length()));
            return;
        }
        if (action.startsWith(ACTION_TWINK_DELETE_YES)) {
            sender.clearKeyboard(chatId, messageId);
            confirmTwinkDelete(telegramId, chatId, action.substring(ACTION_TWINK_DELETE_YES.length()));
            return;
        }
        if (action.startsWith(ACTION_TWINK_DELETE)) {
            requestTwinkDelete(telegramId, chatId, messageId, action.substring(ACTION_TWINK_DELETE.length()));
            return;
        }
        switch (action) {
            case ACTION_OPEN -> sendMenu(chatId);
            case ACTION_MANAGE, ACTION_BACK_MANAGE -> openManage(telegramId, chatId, messageId);
            case ACTION_BACK -> editKeyboard(chatId, messageId, messagesConfig.getTelegramMenuTitle(), mainMenuRows().toArray(new List[0]));
            case ACTION_UNBIND -> requestUnbind(telegramId, chatId, messageId);
            case ACTION_UNBIND_YES -> {
                sender.clearKeyboard(chatId, messageId);
                confirmUnbind(telegramId, chatId);
            }
            case ACTION_UNBIND_NO -> {
                sender.clearKeyboard(chatId, messageId);
                sendResult(chatId, messagesConfig.getTelegramMenuUnbindCancelled());
            }
            case ACTION_PASSWORD -> requestPassword(telegramId, chatId, messageId);
            case ACTION_PASSWORD_YES -> {
                sender.clearKeyboard(chatId, messageId);
                confirmPassword(telegramId, chatId);
            }
            case ACTION_PASSWORD_NO -> {
                sender.clearKeyboard(chatId, messageId);
                sendResult(chatId, messagesConfig.getTelegramMenuPasswordCancelled());
            }
            case ACTION_BANK -> openBank(telegramId, chatId, messageId);
            case ACTION_BANK_TRANSFER -> openBankTransfer(telegramId, chatId, messageId);
            case ACTION_BANK_NO -> {
                sender.clearKeyboard(chatId, messageId);
                sendResult(chatId, skinsBot.bank.cancelled);
            }
            case ACTION_TWINK -> openTwinks(telegramId, chatId, messageId);
            case ACTION_TWINK_ADD -> openTwinkAdd(telegramId, chatId, messageId);
            case ACTION_TWINK_DELETE_NO -> {
                sender.clearKeyboard(chatId, messageId);
                sendResult(chatId, messagesConfig.getTwinkDeleteCancelled());
            }
            case ACTION_SKINS -> skinsMenu.open(telegramId, chatId, messageId);
            default -> sendMenu(chatId);
        }
    }

    private List<List<InlineKeyboardButton>> mainMenuRows() {
        return menuRows(skinsBot.skins.layout.telegramMain, skinsBot.skins.layout.telegramMainPerRow);
    }

    private List<List<InlineKeyboardButton>> menuRows(List<String> keys, int perRow) {
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (String key : keys) {
            InlineKeyboardButton button = menuButton(key);
            if (button != null) {
                buttons.add(button);
            }
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += perRow) {
            rows.add(new ArrayList<>(buttons.subList(i, Math.min(i + perRow, buttons.size()))));
        }
        return rows;
    }

    private InlineKeyboardButton menuButton(String key) {
        return switch (key) {
            case "manage" -> button(messagesConfig.getMenuManageButton(), ACTION_MANAGE);
            case "skins" -> skinsMenu.menuButton();
            case "unbind" -> button(messagesConfig.getMenuUnbindButton(), ACTION_UNBIND);
            case "password" -> passwordService.isAvailable() ? button(messagesConfig.getMenuPasswordButton(), ACTION_PASSWORD) : null;
            case "twink" -> twinkService.isAvailable() ? button(messagesConfig.getMenuTwinkButton(), ACTION_TWINK) : null;
            case "bank" -> bankService.isAvailable() ? button(skinsBot.bank.menuButton, ACTION_BANK) : null;
            case "back" -> button(messagesConfig.getMenuBackButton(), ACTION_BACK);
            default -> null;
        };
    }

    public boolean isAwaitingNick(String telegramId) {
        Long until = awaitingNick.get(telegramId);
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            awaitingNick.remove(telegramId);
            return false;
        }
        return true;
    }

    public void cancelNickInput(String telegramId) {
        awaitingNick.remove(telegramId);
        awaitingTransfer.remove(telegramId);
    }

    public boolean isAwaitingTransfer(String telegramId) {
        Long until = awaitingTransfer.get(telegramId);
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            awaitingTransfer.remove(telegramId);
            return false;
        }
        return true;
    }

    public void handleTransferInput(String telegramId, String chatId, String text) {
        UUID playerUuid = boundPlayer(telegramId, chatId);
        if (playerUuid == null) {
            awaitingTransfer.remove(telegramId);
            return;
        }
        String trimmed = text.trim();
        int split = trimmed.lastIndexOf(' ');
        int amount = split > 0 ? BankService.parseAmount(trimmed.substring(split + 1)) : -1;
        if (split <= 0) {
            retryTransfer(telegramId, chatId, skinsBot.bank.invalidFormat);
            return;
        }
        if (amount < 0) {
            retryTransfer(telegramId, chatId, skinsBot.bank.invalidAmount);
            return;
        }
        String recipient = trimmed.substring(0, split).trim();
        bankService.quote(playerUuid, recipient, amount).thenAccept(quote -> {
            if (quote.status() != BankService.Status.OK) {
                retryTransfer(telegramId, chatId, bankError(quote));
                return;
            }
            awaitingTransfer.remove(telegramId);
            sendKeyboard(chatId, bankText(skinsBot.bank.confirmText, quote),
                    row(button(skinsBot.bank.confirmYesButton,
                                    ACTION_BANK_YES + quote.cardId() + "_" + quote.amount()),
                            button(skinsBot.bank.confirmNoButton, ACTION_BANK_NO)));
        });
    }

    private void retryTransfer(String telegramId, String chatId, String message) {
        awaitingTransfer.put(telegramId, System.currentTimeMillis() + NICK_INPUT_MILLIS);
        sendKeyboard(chatId, message, row(button(messagesConfig.getMenuBackButton(), ACTION_BANK)));
    }

    private void openBank(String telegramId, String chatId, Integer messageId) {
        UUID playerUuid = boundPlayer(telegramId, chatId);
        if (playerUuid == null) {
            return;
        }
        bankService.overview(playerUuid).thenAccept(overview -> {
            switch (overview.status()) {
                case OK -> editKeyboard(chatId, messageId, skinsBot.bank.overviewTitle
                                .replace("{card}", overview.cardId())
                                .replace("{balance}", BankService.format(overview.balance())),
                        row(button(skinsBot.bank.transferButton, ACTION_BANK_TRANSFER)),
                        row(button(messagesConfig.getMenuBackButton(), ACTION_BACK)));
                case NO_CARD -> editKeyboard(chatId, messageId, skinsBot.bank.noCard,
                        row(button(messagesConfig.getMenuBackButton(), ACTION_BACK)));
                default -> editKeyboard(chatId, messageId, skinsBot.bank.unavailable,
                        row(button(messagesConfig.getMenuBackButton(), ACTION_BACK)));
            }
        });
    }

    private void openBankTransfer(String telegramId, String chatId, Integer messageId) {
        if (boundPlayer(telegramId, chatId) == null) {
            return;
        }
        awaitingTransfer.put(telegramId, System.currentTimeMillis() + NICK_INPUT_MILLIS);
        editKeyboard(chatId, messageId, skinsBot.bank.transferPrompt,
                row(button(messagesConfig.getMenuBackButton(), ACTION_BANK)));
    }

    private void confirmBankTransfer(String telegramId, String chatId, String payload) {
        UUID playerUuid = boundPlayer(telegramId, chatId);
        if (playerUuid == null) {
            return;
        }
        int split = payload.lastIndexOf('_');
        int amount = split > 0 ? BankService.parseAmount(payload.substring(split + 1)) : -1;
        if (amount < 0) {
            sendResult(chatId, skinsBot.bank.invalidAmount);
            return;
        }
        bankService.transfer(playerUuid, payload.substring(0, split), amount, "telegram").thenAccept(quote -> {
            if (quote.status() == BankService.Status.OK) {
                sendResult(chatId, bankText(skinsBot.bank.success, quote));
            } else {
                sendResult(chatId, bankError(quote));
            }
        });
    }

    private String bankError(BankService.Quote quote) {
        return switch (quote.status()) {
            case RECIPIENT_NOT_FOUND -> skinsBot.bank.recipientNotFound;
            case OWN_CARD -> skinsBot.bank.ownCard;
            case INVALID_AMOUNT -> skinsBot.bank.invalidAmount;
            case INSUFFICIENT -> bankText(skinsBot.bank.insufficient, quote);
            case NO_CARD -> skinsBot.bank.noCard;
            case UNAVAILABLE -> skinsBot.bank.unavailable;
            default -> skinsBot.bank.transferError;
        };
    }

    private String bankText(String template, BankService.Quote quote) {
        return template
                .replace("{card}", String.valueOf(quote.cardId()))
                .replace("{recipient}", TelegramTextFormatter.escapeHtml(String.valueOf(quote.recipientName())))
                .replace("{amount}", String.valueOf(quote.amount()))
                .replace("{fee}", BankService.format(quote.fee()))
                .replace("{percent}", BankService.format(quote.feePercent()))
                .replace("{total}", BankService.format(quote.total()))
                .replace("{balance}", BankService.format(quote.balance()));
    }

    public void handleNickInput(String telegramId, String chatId, String text) {
        twinkService.invite(BindingType.TELEGRAM, telegramId, text).thenAccept(result -> {
            switch (result.status()) {
                case CREATED -> {
                    awaitingNick.remove(telegramId);
                    sendResult(chatId, messagesConfig.getTwinkCreated(result.twinkName(), result.code(),
                            result.lifetimeMinutes()));
                }
                case LIMIT_REACHED -> {
                    awaitingNick.remove(telegramId);
                    sendResult(chatId, messagesConfig.getTwinkLimit(
                            twinkService.slots(BindingType.TELEGRAM, telegramId).max(),
                            twinkList(twinkService.slots(BindingType.TELEGRAM, telegramId))));
                }
                case NOT_BOUND -> {
                    awaitingNick.remove(telegramId);
                    sendHtml(chatId, messagesConfig.getTelegramMenuNotBound());
                }
                case UNAVAILABLE -> {
                    awaitingNick.remove(telegramId);
                    sendResult(chatId, messagesConfig.getTwinkUnavailable());
                }
                case INVALID_NAME -> retryNick(telegramId, chatId, messagesConfig.getTwinkInvalidName());
                case ALREADY_BOUND -> retryNick(telegramId, chatId,
                        messagesConfig.getTwinkAlreadyBound(result.twinkName()));
                case ALREADY_PENDING -> retryNick(telegramId, chatId,
                        messagesConfig.getTwinkPending(result.twinkName()));
                case NOT_SEEN -> retryNick(telegramId, chatId, messagesConfig.getTwinkNotSeen(result.twinkName()));
            }
        }).exceptionally(error -> {
            awaitingNick.remove(telegramId);
            sendResult(chatId, messagesConfig.getTwinkUnavailable());
            return null;
        });
    }

    private void retryNick(String telegramId, String chatId, String message) {
        awaitingNick.put(telegramId, System.currentTimeMillis() + NICK_INPUT_MILLIS);
        sendKeyboard(chatId, message, row(button(messagesConfig.getMenuBackButton(), ACTION_TWINK)));
    }

    private void openTwinks(String telegramId, String chatId, Integer messageId) {
        if (boundPlayer(telegramId, chatId) == null) {
            return;
        }
        if (!twinkService.isAvailable()) {
            sendResult(chatId, messagesConfig.getTwinkUnavailable());
            return;
        }
        Slots slots = twinkService.slots(BindingType.TELEGRAM, telegramId);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (slots.free() > 0) {
            rows.add(row(button(messagesConfig.getTwinkAddButton(), ACTION_TWINK_ADD)));
        }
        for (TwinkService.TwinkEntry entry : slots.entries()) {
            rows.add(row(button(messagesConfig.getTwinkDeleteButton(entry.name()), ACTION_TWINK_DELETE + entry.name())));
        }
        rows.add(row(button(messagesConfig.getMenuBackButton(), ACTION_BACK)));
        editKeyboard(chatId, messageId, messagesConfig.getTwinksTitle(slots.free(), slots.max(), twinkList(slots)),
                rows.toArray(new List[0]));
    }

    private void openTwinkAdd(String telegramId, String chatId, Integer messageId) {
        if (boundPlayer(telegramId, chatId) == null) {
            return;
        }
        if (!twinkService.isAvailable()) {
            sendResult(chatId, messagesConfig.getTwinkUnavailable());
            return;
        }
        Slots slots = twinkService.slots(BindingType.TELEGRAM, telegramId);
        if (slots.free() == 0) {
            editKeyboard(chatId, messageId, messagesConfig.getTwinkLimit(slots.max(), twinkList(slots)),
                    row(button(messagesConfig.getMenuBackButton(), ACTION_TWINK)));
            return;
        }
        awaitingNick.put(telegramId, System.currentTimeMillis() + NICK_INPUT_MILLIS);
        editKeyboard(chatId, messageId, messagesConfig.getTwinkPrompt(slots.free(), slots.max(), twinkList(slots)),
                row(button(messagesConfig.getMenuBackButton(), ACTION_TWINK)));
    }

    private void requestTwinkDelete(String telegramId, String chatId, Integer messageId, String twinkName) {
        if (boundPlayer(telegramId, chatId) == null) {
            return;
        }
        boolean known = twinkService.slots(BindingType.TELEGRAM, telegramId).entries().stream()
                .anyMatch(entry -> entry.name().equalsIgnoreCase(twinkName));
        if (!known) {
            sendResult(chatId, messagesConfig.getTwinkDeleteNotFound(twinkName));
            return;
        }
        editKeyboard(chatId, messageId, messagesConfig.getTwinkDeleteConfirm(twinkName),
                row(button(messagesConfig.getTwinkDeleteYesButton(), ACTION_TWINK_DELETE_YES + twinkName),
                        button(messagesConfig.getTwinkDeleteNoButton(), ACTION_TWINK_DELETE_NO)),
                row(button(messagesConfig.getMenuBackButton(), ACTION_TWINK)));
    }

    private void confirmTwinkDelete(String telegramId, String chatId, String twinkName) {
        switch (twinkService.remove(BindingType.TELEGRAM, telegramId, twinkName)) {
            case REMOVED -> sendResult(chatId, messagesConfig.getTwinkDeleted(twinkName));
            case NOT_FOUND -> sendResult(chatId, messagesConfig.getTwinkDeleteNotFound(twinkName));
            case NOT_BOUND -> sendHtml(chatId, messagesConfig.getTelegramMenuNotBound());
        }
    }

    private String twinkList(Slots slots) {
        if (slots.entries().isEmpty()) {
            return messagesConfig.getTwinkListEmpty();
        }
        StringBuilder builder = new StringBuilder();
        for (TwinkService.TwinkEntry entry : slots.entries()) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(messagesConfig.getTwinkListItem(entry.name(), entry.pending()));
        }
        return builder.toString();
    }

    private void openManage(String telegramId, String chatId, Integer messageId) {
        if (boundPlayer(telegramId, chatId) == null) {
            return;
        }
        editKeyboard(chatId, messageId, messagesConfig.getMenuManageTitle(),
                menuRows(skinsBot.skins.layout.telegramManage, skinsBot.skins.layout.telegramManagePerRow).toArray(new List[0]));
    }

    private void requestUnbind(String telegramId, String chatId, Integer messageId) {
        UUID playerUuid = boundPlayer(telegramId, chatId);
        if (playerUuid == null) {
            return;
        }
        editKeyboard(chatId, messageId, messagesConfig.getTelegramMenuUnbindConfirm(resolveName(playerUuid)),
                row(button(messagesConfig.getMenuUnbindYesButton(), ACTION_UNBIND_YES),
                        button(messagesConfig.getMenuUnbindNoButton(), ACTION_UNBIND_NO)),
                row(button(messagesConfig.getMenuBackButton(), ACTION_BACK_MANAGE)));
    }

    private void confirmUnbind(String telegramId, String chatId) {
        UUID playerUuid = boundPlayer(telegramId, chatId);
        if (playerUuid == null) {
            return;
        }
        String playerName = resolveName(playerUuid);

        if (bindingService.unbindAccount(playerUuid)) {
            sendResult(chatId, messagesConfig.getTelegramMenuUnbindSuccess(playerName));
        } else {
            sendResult(chatId, messagesConfig.getTelegramMenuUnbindError());
        }
    }

    private void requestPassword(String telegramId, String chatId, Integer messageId) {
        UUID playerUuid = boundPlayer(telegramId, chatId);
        if (playerUuid == null) {
            return;
        }
        if (!passwordService.isAvailable()) {
            sendResult(chatId, messagesConfig.getTelegramMenuPasswordUnavailable());
            return;
        }
        editKeyboard(chatId, messageId, messagesConfig.getTelegramMenuPasswordConfirm(resolveName(playerUuid)),
                row(button(messagesConfig.getMenuPasswordYesButton(), ACTION_PASSWORD_YES),
                        button(messagesConfig.getMenuPasswordNoButton(), ACTION_PASSWORD_NO)),
                row(button(messagesConfig.getMenuBackButton(), ACTION_BACK_MANAGE)));
    }

    private void confirmPassword(String telegramId, String chatId) {
        passwordService.changePassword(BindingType.TELEGRAM, telegramId).thenAccept(result -> {
            switch (result.status()) {
                case OK -> sendResult(chatId,
                        messagesConfig.getTelegramMenuPasswordSuccess(result.playerName(), result.password()));
                case NOT_BOUND -> sendHtml(chatId, messagesConfig.getTelegramMenuNotBound());
                case NOT_REGISTERED -> sendResult(chatId,
                        messagesConfig.getTelegramMenuPasswordNotRegistered(result.playerName()));
                case UNAVAILABLE -> sendResult(chatId, messagesConfig.getTelegramMenuPasswordUnavailable());
            }
        }).exceptionally(error -> {
            sendResult(chatId, messagesConfig.getTelegramMenuPasswordError());
            return null;
        });
    }

    private UUID boundPlayer(String telegramId, String chatId) {
        UUID playerUuid = bindingService.getPlayerByBinding(BindingType.TELEGRAM, telegramId);
        if (playerUuid == null) {
            sendHtml(chatId, messagesConfig.getTelegramMenuNotBound());
        }
        return playerUuid;
    }

    private String resolveName(UUID playerUuid) {
        String name = bindingService.getPlayerName(playerUuid);
        return name != null && !name.isBlank() ? name : "?";
    }

    private void sendResult(String chatId, String rawMessage) {
        sendKeyboard(chatId, rawMessage, row(button(messagesConfig.getMenuOpenButton(), ACTION_OPEN)));
    }

    @SafeVarargs
    private void editKeyboard(String chatId, Integer messageId, String rawMessage, List<InlineKeyboardButton>... rows) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(new ArrayList<>(List.of(rows)));
        sender.editMessage(chatId, messageId, TelegramTextFormatter.convertMarkdownToHtml(rawMessage), true, keyboard);
    }

    @SafeVarargs
    private void sendKeyboard(String chatId, String rawMessage, List<InlineKeyboardButton>... rows) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(new ArrayList<>(List.of(rows)));
        sender.sendMessage(chatId, TelegramTextFormatter.convertMarkdownToHtml(rawMessage), true, keyboard);
    }

    private InlineKeyboardButton button(String text, String action) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(CALLBACK_PREFIX + action);
        return button;
    }

    private List<InlineKeyboardButton> row(InlineKeyboardButton... buttons) {
        return new ArrayList<>(List.of(buttons));
    }

    private void sendHtml(String chatId, String rawMessage) {
        sender.sendMessage(chatId, TelegramTextFormatter.convertMarkdownToHtml(rawMessage), true);
    }
}
