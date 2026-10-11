package z.caAuth.bots.discord;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import z.caAuth.account.BindingType;
import z.caAuth.banking.BankService;
import z.caAuth.binding.BindingService;
import z.caAuth.binding.PasswordService;
import z.caAuth.binding.TwinkService;
import z.caAuth.binding.TwinkService.Slots;
import z.caAuth.bots.discord.role.DiscordRoleService;
import z.caAuth.config.DiscordBotMessages;
import z.caAuth.config.DiscordBotMessages.ButtonKey;
import z.caAuth.config.DiscordBotMessages.EmbedType;
import z.caAuth.config.SkinsBotMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class DiscordMenu {

    public static final String PREFIX = "menu_";
    public static final String COMMAND_NAME = "menu";

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
    private static final String ACTION_SYNC = "sync";
    private static final String ACTION_BANK = "bank";
    private static final String ACTION_BANK_TRANSFER = "bank_transfer";
    private static final String ACTION_BANK_YES = "bank_yes_";
    private static final String ACTION_BANK_NO = "bank_no";
    private static final String MODAL_BANK = PREFIX + "bank_modal";
    private static final String MODAL_BANK_RECIPIENT = "recipient";
    private static final String MODAL_BANK_AMOUNT = "amount";
    private static final String ACTION_TWINK = "twink";
    private static final String ACTION_TWINK_ADD = "twink_add";
    private static final String ACTION_TWINK_DELETE = "twink_del_";
    private static final String ACTION_TWINK_DELETE_YES = "twink_delyes_";
    private static final String ACTION_TWINK_DELETE_NO = "twink_delno";
    private static final String ACTION_SKINS = DiscordSkinsMenu.MENU_ACTION;
    private static final int ROW_SIZE = 5;
    private static final int MAX_ROWS = 5;
    private static final String MODAL_TWINK = PREFIX + "twink_modal";
    private static final char LINE_BREAK = (char) 10;
    private static final String MODAL_NICK_FIELD = "nick";

    private final BindingService bindingService;
    private final PasswordService passwordService;
    private final TwinkService twinkService;
    private final DiscordBotMessages messages;
    private final DiscordEmbeds embeds;
    private final DiscordSkinsMenu skinsMenu;
    private final SkinsBotMessages skinsBot;
    private final DiscordRoleService roleService;
    private final BankService bankService;

    public DiscordMenu(BindingService bindingService, PasswordService passwordService, TwinkService twinkService,
                       DiscordBotMessages messages, DiscordEmbeds embeds, DiscordSkinsMenu skinsMenu,
                       SkinsBotMessages skinsBot, DiscordRoleService roleService, BankService bankService) {
        this.bankService = bankService;
        this.roleService = roleService;
        this.bindingService = bindingService;
        this.passwordService = passwordService;
        this.twinkService = twinkService;
        this.messages = messages;
        this.embeds = embeds;
        this.skinsMenu = skinsMenu;
        this.skinsBot = skinsBot;
    }

    public boolean isMenuText(String text) {
        String lower = text.toLowerCase();
        return lower.equals("/" + COMMAND_NAME) || lower.equals(COMMAND_NAME);
    }

    public boolean isMenuCallback(String componentId) {
        return componentId != null && componentId.startsWith(PREFIX);
    }

    public void sendMenu(MessageChannel channel) {
        channel.sendMessageEmbeds(mainEmbed()).setComponents(menuRows(skinsBot.skins.layout.discordMain, skinsBot.skins.layout.discordMainPerRow)).queue();
    }

    public void replyMenu(IReplyCallback event) {
        event.replyEmbeds(mainEmbed()).setComponents(menuRows(skinsBot.skins.layout.discordMain, skinsBot.skins.layout.discordMainPerRow)).queue();
    }

    public void handleButton(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        String action = event.getComponentId().substring(PREFIX.length());
        if (action.startsWith(ACTION_BANK_YES)) {
            String payload = action.substring(ACTION_BANK_YES.length());
            acknowledge(event, hook -> confirmBankTransfer(hook, userId, payload));
            return;
        }
        if (action.startsWith(ACTION_TWINK_DELETE_YES)) {
            String twinkName = action.substring(ACTION_TWINK_DELETE_YES.length());
            acknowledge(event, hook -> confirmTwinkDelete(hook, userId, twinkName));
            return;
        }
        if (action.startsWith(ACTION_TWINK_DELETE)) {
            requestTwinkDelete(event, userId, action.substring(ACTION_TWINK_DELETE.length()));
            return;
        }
        switch (action) {
            case ACTION_OPEN -> replyMenu(event);
            case ACTION_MANAGE, ACTION_BACK_MANAGE -> openManage(event, userId);
            case ACTION_BACK -> event.editMessageEmbeds(mainEmbed())
                    .setComponents(menuRows(skinsBot.skins.layout.discordMain, skinsBot.skins.layout.discordMainPerRow)).queue();
            case ACTION_UNBIND -> requestUnbind(event, userId);
            case ACTION_UNBIND_YES -> acknowledge(event, hook -> confirmUnbind(hook, userId));
            case ACTION_UNBIND_NO -> acknowledge(event,
                    hook -> sendResult(hook, EmbedType.INFO, messages.getMenuUnbindCancelled()));
            case ACTION_PASSWORD -> requestPassword(event, userId);
            case ACTION_PASSWORD_YES -> acknowledge(event, hook -> confirmPassword(hook, userId));
            case ACTION_PASSWORD_NO -> acknowledge(event,
                    hook -> sendResult(hook, EmbedType.INFO, messages.getMenuPasswordCancelled()));
            case ACTION_SYNC -> sync(event, userId);
            case ACTION_BANK -> openBank(event, userId);
            case ACTION_BANK_TRANSFER -> openBankTransfer(event, userId);
            case ACTION_BANK_NO -> acknowledge(event,
                    hook -> sendResult(hook, EmbedType.INFO, skinsBot.bank.cancelled));
            case ACTION_TWINK -> openTwinks(event, userId);
            case ACTION_TWINK_ADD -> openTwinkAdd(event, userId);
            case ACTION_TWINK_DELETE_NO -> acknowledge(event,
                    hook -> sendResult(hook, EmbedType.INFO, messages.getTwinkDeleteCancelled()));
            case ACTION_SKINS -> skinsMenu.open(event, userId);
            default -> event.replyEmbeds(embeds.embed(EmbedType.ERROR, messages.getMenuUnknownAction())).queue();
        }
    }

    public boolean isMenuModal(String modalId) {
        return MODAL_TWINK.equals(modalId) || MODAL_BANK.equals(modalId);
    }

    public void handleModal(ModalInteractionEvent event) {
        String userId = event.getUser().getId();
        if (MODAL_BANK.equals(event.getModalId())) {
            handleBankModal(event, userId);
            return;
        }
        String nick = event.getValue(MODAL_NICK_FIELD) != null ? event.getValue(MODAL_NICK_FIELD).getAsString() : "";
        event.deferReply().queue(hook -> twinkService.invite(BindingType.DISCORD, userId, nick).thenAccept(result -> {
            switch (result.status()) {
                case CREATED -> sendResult(hook, EmbedType.SUCCESS,
                        messages.getTwinkCreated(result.twinkName(), result.code(), result.lifetimeMinutes()));
                case LIMIT_REACHED -> {
                    Slots slots = twinkService.slots(BindingType.DISCORD, userId);
                    sendResult(hook, EmbedType.ERROR, messages.getTwinkLimit(slots.max(), twinkList(slots)));
                }
                case NOT_BOUND -> send(hook, EmbedType.ERROR, messages.getMenuNotBound());
                case UNAVAILABLE -> sendResult(hook, EmbedType.ERROR, messages.getTwinkUnavailable());
                case INVALID_NAME -> sendResult(hook, EmbedType.ERROR, messages.getTwinkInvalidName());
                case ALREADY_BOUND -> sendResult(hook, EmbedType.ERROR,
                        messages.getTwinkAlreadyBound(result.twinkName()));
                case ALREADY_PENDING -> sendResult(hook, EmbedType.WARNING,
                        messages.getTwinkPending(result.twinkName()));
                case NOT_SEEN -> sendResult(hook, EmbedType.ERROR, messages.getTwinkNotSeen(result.twinkName()));
            }
        }).exceptionally(error -> {
            sendResult(hook, EmbedType.ERROR, messages.getTwinkUnavailable());
            return null;
        }));
    }

    private void openTwinks(ButtonInteractionEvent event, String userId) {
        if (boundPlayer(event, userId) == null) {
            return;
        }
        if (!twinkService.isAvailable()) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, messages.getTwinkUnavailable()))
                    .addActionRow(openButton()).queue();
            return;
        }
        Slots slots = twinkService.slots(BindingType.DISCORD, userId);
        List<Button> buttons = new ArrayList<>();
        if (slots.free() > 0) {
            buttons.add(embeds.button(ButtonKey.MENU_TWINK_ADD, PREFIX + ACTION_TWINK_ADD, messages.getTwinkAddButton()));
        }
        for (TwinkService.TwinkEntry entry : slots.entries()) {
            buttons.add(embeds.button(ButtonKey.MENU_TWINK_DELETE, PREFIX + ACTION_TWINK_DELETE + entry.name(),
                    messages.getTwinkDeleteButton(entry.name())));
        }
        buttons.add(backButton(ACTION_BACK));
        event.editMessageEmbeds(embeds.embed(EmbedType.INFO,
                        messages.getTwinksTitle(slots.free(), slots.max(), twinkList(slots))))
                .setComponents(rows(buttons))
                .queue();
    }

    private List<ActionRow> rows(List<Button> buttons) {
        return rows(buttons, ROW_SIZE);
    }

    private List<ActionRow> rows(List<Button> buttons, int perRow) {
        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size() && rows.size() < MAX_ROWS; i += perRow) {
            rows.add(ActionRow.of(buttons.subList(i, Math.min(i + perRow, buttons.size()))));
        }
        return rows;
    }

    private void requestTwinkDelete(ButtonInteractionEvent event, String userId, String twinkName) {
        if (boundPlayer(event, userId) == null) {
            return;
        }
        boolean known = twinkService.slots(BindingType.DISCORD, userId).entries().stream()
                .anyMatch(entry -> entry.name().equalsIgnoreCase(twinkName));
        if (!known) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, messages.getTwinkDeleteNotFound(twinkName)))
                    .addActionRow(openButton()).queue();
            return;
        }
        event.editMessageEmbeds(embeds.embed(EmbedType.WARNING, messages.getTwinkDeleteConfirm(twinkName)))
                .setComponents(ActionRow.of(
                        embeds.button(ButtonKey.MENU_TWINK_DELETE_YES, PREFIX + ACTION_TWINK_DELETE_YES + twinkName,
                                messages.getTwinkDeleteYesButton()),
                        embeds.button(ButtonKey.MENU_TWINK_DELETE_NO, PREFIX + ACTION_TWINK_DELETE_NO,
                                messages.getTwinkDeleteNoButton()),
                        backButton(ACTION_TWINK)))
                .queue();
    }

    private void confirmTwinkDelete(InteractionHook hook, String userId, String twinkName) {
        switch (twinkService.remove(BindingType.DISCORD, userId, twinkName)) {
            case REMOVED -> sendResult(hook, EmbedType.SUCCESS, messages.getTwinkDeleted(twinkName));
            case NOT_FOUND -> sendResult(hook, EmbedType.ERROR, messages.getTwinkDeleteNotFound(twinkName));
            case NOT_BOUND -> send(hook, EmbedType.ERROR, messages.getMenuNotBound());
        }
    }

    private void openTwinkAdd(ButtonInteractionEvent event, String userId) {
        if (boundPlayer(event, userId) == null) {
            return;
        }
        if (!twinkService.isAvailable()) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, messages.getTwinkUnavailable()))
                    .addActionRow(openButton()).queue();
            return;
        }
        Slots slots = twinkService.slots(BindingType.DISCORD, userId);
        if (slots.free() == 0) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, messages.getTwinkLimit(slots.max(), twinkList(slots))))
                    .addActionRow(openButton()).queue();
            return;
        }
        TextInput input = TextInput.create(MODAL_NICK_FIELD, messages.getTwinkModalLabel(), TextInputStyle.SHORT)
                .setPlaceholder(messages.getTwinkModalPlaceholder())
                .setMinLength(3)
                .setMaxLength(16)
                .setRequired(true)
                .build();
        event.replyModal(Modal.create(MODAL_TWINK, messages.getTwinkModalTitle()).addActionRow(input).build()).queue();
    }

    private String twinkList(Slots slots) {
        if (slots.entries().isEmpty()) {
            return messages.getTwinkListEmpty();
        }
        StringBuilder builder = new StringBuilder();
        for (TwinkService.TwinkEntry entry : slots.entries()) {
            if (!builder.isEmpty()) {
                builder.append(LINE_BREAK);
            }
            builder.append(messages.getTwinkListItem(entry.name(), entry.pending()));
        }
        return builder.toString();
    }

    private MessageEmbed mainEmbed() {
        return embeds.embed(EmbedType.INFO, messages.getMenuTitle());
    }

    private List<ActionRow> menuRows(List<String> keys, int perRow) {
        List<Button> buttons = new ArrayList<>();
        for (String key : keys) {
            Button button = menuButton(key);
            if (button != null) {
                buttons.add(button);
            }
        }
        return rows(buttons, perRow);
    }

    private Button menuButton(String key) {
        return switch (key) {
            case "manage" -> embeds.button(ButtonKey.MENU_MANAGE, PREFIX + ACTION_MANAGE, messages.getMenuManageButton());
            case "skins" -> skinsMenu.menuButton();
            case "unbind" -> embeds.button(ButtonKey.MENU_UNBIND, PREFIX + ACTION_UNBIND, messages.getMenuUnbindButton());
            case "password" -> embeds.button(ButtonKey.MENU_PASSWORD, PREFIX + ACTION_PASSWORD, messages.getMenuPasswordButton());
            case "bank" -> bankService.isAvailable()
                    ? embeds.button(skinsBot.bank.discordMenuStyle, PREFIX + ACTION_BANK, skinsBot.bank.menuButton)
                    : null;
            case "sync" ->embeds.button(ButtonKey.MENU_SYNC, PREFIX + ACTION_SYNC, messages.getMenuSyncButton());
            case "twink" -> embeds.button(ButtonKey.MENU_TWINK, PREFIX + ACTION_TWINK, messages.getMenuTwinkButton());
            case "back" -> backButton(ACTION_BACK);
            default -> null;
        };
    }

    private Button openButton() {
        return embeds.button(ButtonKey.MENU_OPEN, PREFIX + ACTION_OPEN, messages.getMenuOpenButton());
    }

    private Button backButton(String action) {
        return embeds.button(ButtonKey.MENU_BACK, PREFIX + action, messages.getMenuBackButton());
    }

    private void openManage(ButtonInteractionEvent event, String userId) {
        if (boundPlayer(event, userId) == null) {
            return;
        }
        event.editMessageEmbeds(embeds.embed(EmbedType.INFO, messages.getMenuManageTitle()))
                .setComponents(menuRows(skinsBot.skins.layout.discordManage, skinsBot.skins.layout.discordManagePerRow))
                .queue();
    }

    private void requestUnbind(ButtonInteractionEvent event, String userId) {
        UUID playerUuid = boundPlayer(event, userId);
        if (playerUuid == null) {
            return;
        }
        event.editMessageEmbeds(embeds.embed(EmbedType.WARNING, messages.getMenuUnbindConfirm(resolveName(playerUuid))))
                .setComponents(ActionRow.of(
                        embeds.button(ButtonKey.MENU_UNBIND_YES, PREFIX + ACTION_UNBIND_YES, messages.getMenuUnbindYesButton()),
                        embeds.button(ButtonKey.MENU_UNBIND_NO, PREFIX + ACTION_UNBIND_NO, messages.getMenuUnbindNoButton()),
                        backButton(ACTION_BACK_MANAGE)))
                .queue();
    }

    private void confirmUnbind(InteractionHook hook, String userId) {
        UUID playerUuid = bindingService.getPlayerByBinding(BindingType.DISCORD, userId);
        if (playerUuid == null) {
            send(hook, EmbedType.ERROR, messages.getMenuNotBound());
            return;
        }
        String playerName = resolveName(playerUuid);
        if (bindingService.unbindAccount(playerUuid)) {
            sendResult(hook, EmbedType.SUCCESS, messages.getMenuUnbindSuccess(playerName));
        } else {
            sendResult(hook, EmbedType.ERROR, messages.getMenuUnbindError());
        }
    }

    private void openBank(ButtonInteractionEvent event, String userId) {
        UUID playerUuid = boundPlayer(event, userId);
        if (playerUuid == null) {
            return;
        }
        event.deferEdit().queue(hook -> bankService.overview(playerUuid).thenAccept(overview -> {
            switch (overview.status()) {
                case OK -> hook.editOriginalEmbeds(embeds.embed(EmbedType.INFO, skinsBot.bank.overviewTitle
                                .replace("{card}", overview.cardId())
                                .replace("{balance}", BankService.format(overview.balance()))))
                        .setComponents(ActionRow.of(
                                embeds.button(skinsBot.bank.discordTransferStyle, PREFIX + ACTION_BANK_TRANSFER,
                                        skinsBot.bank.transferButton),
                                backButton(ACTION_BACK))).queue();
                case NO_CARD -> hook.editOriginalEmbeds(embeds.embed(EmbedType.ERROR, skinsBot.bank.noCard))
                        .setComponents(ActionRow.of(backButton(ACTION_BACK))).queue();
                default -> hook.editOriginalEmbeds(embeds.embed(EmbedType.ERROR, skinsBot.bank.unavailable))
                        .setComponents(ActionRow.of(backButton(ACTION_BACK))).queue();
            }
        }));
    }

    private void openBankTransfer(ButtonInteractionEvent event, String userId) {
        if (boundPlayer(event, userId) == null) {
            return;
        }
        TextInput recipient = TextInput.create(MODAL_BANK_RECIPIENT, skinsBot.bank.modalRecipientLabel, TextInputStyle.SHORT)
                .setPlaceholder(skinsBot.bank.modalRecipientPlaceholder)
                .setMinLength(3)
                .setMaxLength(32)
                .setRequired(true)
                .build();
        TextInput amount = TextInput.create(MODAL_BANK_AMOUNT, skinsBot.bank.modalAmountLabel, TextInputStyle.SHORT)
                .setPlaceholder(skinsBot.bank.modalAmountPlaceholder)
                .setMinLength(1)
                .setMaxLength(7)
                .setRequired(true)
                .build();
        event.replyModal(Modal.create(MODAL_BANK, skinsBot.bank.modalTitle)
                .addActionRow(recipient).addActionRow(amount).build()).queue();
    }

    private void handleBankModal(ModalInteractionEvent event, String userId) {
        UUID playerUuid = bindingService.getPlayerByBinding(BindingType.DISCORD, userId);
        String recipient = event.getValue(MODAL_BANK_RECIPIENT) != null
                ? event.getValue(MODAL_BANK_RECIPIENT).getAsString().trim() : "";
        String rawAmount = event.getValue(MODAL_BANK_AMOUNT) != null
                ? event.getValue(MODAL_BANK_AMOUNT).getAsString() : "";
        event.deferReply().queue(hook -> {
            if (playerUuid == null) {
                send(hook, EmbedType.ERROR, messages.getMenuNotBound());
                return;
            }
            int amount = BankService.parseAmount(rawAmount);
            if (amount < 0) {
                sendResult(hook, EmbedType.ERROR, skinsBot.bank.invalidAmount);
                return;
            }
            bankService.quote(playerUuid, recipient, amount).thenAccept(quote -> {
                if (quote.status() != BankService.Status.OK) {
                    sendResult(hook, EmbedType.ERROR, bankError(quote));
                    return;
                }
                hook.sendMessageEmbeds(embeds.embed(EmbedType.WARNING, bankText(skinsBot.bank.confirmText, quote)))
                        .setComponents(ActionRow.of(
                                embeds.button(skinsBot.bank.discordYesStyle,
                                        PREFIX + ACTION_BANK_YES + quote.cardId() + "_" + quote.amount(),
                                        skinsBot.bank.confirmYesButton),
                                embeds.button(skinsBot.bank.discordNoStyle, PREFIX + ACTION_BANK_NO,
                                        skinsBot.bank.confirmNoButton)))
                        .queue();
            });
        });
    }

    private void confirmBankTransfer(InteractionHook hook, String userId, String payload) {
        UUID playerUuid = bindingService.getPlayerByBinding(BindingType.DISCORD, userId);
        if (playerUuid == null) {
            send(hook, EmbedType.ERROR, messages.getMenuNotBound());
            return;
        }
        int split = payload.lastIndexOf('_');
        int amount = split > 0 ? BankService.parseAmount(payload.substring(split + 1)) : -1;
        if (amount < 0) {
            sendResult(hook, EmbedType.ERROR, skinsBot.bank.invalidAmount);
            return;
        }
        bankService.transfer(playerUuid, payload.substring(0, split), amount, "discord").thenAccept(quote -> {
            if (quote.status() == BankService.Status.OK) {
                sendResult(hook, EmbedType.SUCCESS, bankText(skinsBot.bank.success, quote));
            } else {
                sendResult(hook, EmbedType.ERROR, bankError(quote));
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
                .replace("{recipient}", String.valueOf(quote.recipientName()))
                .replace("{amount}", String.valueOf(quote.amount()))
                .replace("{fee}", BankService.format(quote.fee()))
                .replace("{percent}", BankService.format(quote.feePercent()))
                .replace("{total}", BankService.format(quote.total()))
                .replace("{balance}", BankService.format(quote.balance()));
    }

    private void sync(ButtonInteractionEvent event, String userId) {
        UUID playerUuid = boundPlayer(event, userId);
        if (playerUuid == null) {
            return;
        }
        String playerName = resolveName(playerUuid);
        event.deferReply().queue(hook -> roleService.syncBinding(userId, playerName).thenAccept(done -> {
            if (done) {
                sendResult(hook, EmbedType.SUCCESS, messages.getMenuSyncSuccess(playerName));
            } else {
                sendResult(hook, EmbedType.ERROR, messages.getMenuSyncUnavailable());
            }
        }));
    }

    private void requestPassword(ButtonInteractionEvent event, String userId) {
        UUID playerUuid = boundPlayer(event, userId);
        if (playerUuid == null) {
            return;
        }
        if (!passwordService.isAvailable()) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, messages.getMenuPasswordUnavailable()))
                    .addActionRow(openButton()).queue();
            return;
        }
        event.editMessageEmbeds(embeds.embed(EmbedType.WARNING, messages.getMenuPasswordConfirm(resolveName(playerUuid))))
                .setComponents(ActionRow.of(
                        embeds.button(ButtonKey.MENU_PASSWORD_YES, PREFIX + ACTION_PASSWORD_YES, messages.getMenuPasswordYesButton()),
                        embeds.button(ButtonKey.MENU_PASSWORD_NO, PREFIX + ACTION_PASSWORD_NO, messages.getMenuPasswordNoButton()),
                        backButton(ACTION_BACK_MANAGE)))
                .queue();
    }

    private void confirmPassword(InteractionHook hook, String userId) {
        passwordService.changePassword(BindingType.DISCORD, userId).thenAccept(result -> {
            switch (result.status()) {
                case OK -> sendResult(hook, EmbedType.SUCCESS,
                        messages.getMenuPasswordSuccess(result.playerName(), result.password()));
                case NOT_BOUND -> send(hook, EmbedType.ERROR, messages.getMenuNotBound());
                case NOT_REGISTERED -> sendResult(hook, EmbedType.ERROR,
                        messages.getMenuPasswordNotRegistered(result.playerName()));
                case UNAVAILABLE -> sendResult(hook, EmbedType.ERROR, messages.getMenuPasswordUnavailable());
            }
        }).exceptionally(error -> {
            sendResult(hook, EmbedType.ERROR, messages.getMenuPasswordError());
            return null;
        });
    }

    private UUID boundPlayer(ButtonInteractionEvent event, String userId) {
        UUID playerUuid = bindingService.getPlayerByBinding(BindingType.DISCORD, userId);
        if (playerUuid == null) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, messages.getMenuNotBound())).queue();
        }
        return playerUuid;
    }

    private void acknowledge(ButtonInteractionEvent event, Consumer<InteractionHook> then) {
        event.editComponents(Collections.emptyList()).queue(then);
    }

    private void send(InteractionHook hook, EmbedType type, String message) {
        hook.sendMessageEmbeds(embeds.embed(type, message)).queue();
    }

    private void sendResult(InteractionHook hook, EmbedType type, String message) {
        hook.sendMessageEmbeds(embeds.embed(type, message)).addActionRow(openButton()).queue();
    }

    private String resolveName(UUID playerUuid) {
        String name = bindingService.getPlayerName(playerUuid);
        return name != null && !name.isBlank() ? name : "?";
    }
}
