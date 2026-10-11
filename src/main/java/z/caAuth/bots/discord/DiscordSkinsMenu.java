package z.caAuth.bots.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import z.caAuth.account.BindingType;
import z.caAuth.account.SkinProfile;
import z.caAuth.account.SkinSource;
import z.caAuth.binding.BindingService;
import z.caAuth.config.DiscordBotMessages.EmbedType;
import z.caAuth.config.SkinsBotMessages;
import z.caAuth.config.SkinsConfig;
import z.caAuth.skins.SkinService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DiscordSkinsMenu {

    public static final String MENU_ACTION = "skins";

    private static final String PREFIX = "skin_";
    private static final String MENU_PREFIX = "menu_";
    private static final String ACTION_OPEN = "open";
    private static final String ACTION_ACCOUNT = "acc_";
    private static final String ACTION_VIEW = "view_";
    private static final String ACTION_EDIT = "edit_";
    private static final String ACTION_TYPE = "type_";
    private static final String ACTION_DELETE = "del_";
    private static final String ACTION_DELETE_YES = "delyes_";
    private static final String ACTION_DELETE_NO = "delno_";
    private static final String MODAL_PREFIX = PREFIX + "modal_";
    private static final String MODAL_FIELD = "value";
    private static final String KIND_NICK = "nick";
    private static final String KIND_URL = "url";
    private static final String KIND_PNG = "png";
    private static final String MENU_BACK = MENU_PREFIX + "back";
    private static final String MENU_OPEN = MENU_PREFIX + "open";
    private static final int ROW_SIZE = 5;
    private static final int MAX_ROWS = 5;
    private static final int MAX_LABEL_LENGTH = 40;
    private static final int MAX_NICK_LENGTH = 16;
    private static final int MIN_NICK_LENGTH = 3;
    private static final int MAX_URL_LENGTH = 512;
    private static final int DOWNLOAD_TIMEOUT_SECONDS = 30;
    private static final long MILLIS_PER_SECOND = 1000L;

    private record PendingPng(UUID account, int slot, long expiresAt) {
    }

    private final BindingService bindingService;
    private final SkinService skins;
    private final SkinsConfig config;
    private final SkinsBotMessages botMessages;
    private final DiscordEmbeds embeds;
    private final Map<String, PendingPng> pendingPng = new ConcurrentHashMap<>();

    public DiscordSkinsMenu(BindingService bindingService, SkinService skins, SkinsConfig config,
                            SkinsBotMessages botMessages, DiscordEmbeds embeds) {
        this.bindingService = bindingService;
        this.skins = skins;
        this.config = config;
        this.botMessages = botMessages;
        this.embeds = embeds;
    }

    public Button menuButton() {
        return button(botMessages.skins.discordButtons.menu, MENU_PREFIX + MENU_ACTION, botMessages.skins.bot.menuButton);
    }

    public boolean isSkinsCallback(String componentId) {
        return componentId != null && componentId.startsWith(PREFIX);
    }

    public boolean isSkinsModal(String modalId) {
        return modalId != null && modalId.startsWith(MODAL_PREFIX);
    }

    public boolean hasPendingPng(String userId) {
        PendingPng pending = pendingPng.get(userId);
        if (pending == null) {
            return false;
        }
        if (pending.expiresAt() < System.currentTimeMillis()) {
            pendingPng.remove(userId);
            return false;
        }
        return true;
    }

    public void open(ButtonInteractionEvent event, String userId) {
        UUID owner = boundPlayer(event, userId);
        if (owner == null) {
            return;
        }
        if (!skins.isAvailable()) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, botMessages.skins.bot.unavailable)).queue();
            return;
        }
        event.editMessageEmbeds(embeds.embed(EmbedType.INFO, botMessages.skins.bot.accountTitle))
                .setComponents(accountRows(owner))
                .queue();
    }

    public void sendPngRequest(MessageChannel channel, String userId, UUID account, int slot) {
        long expiresAt = System.currentTimeMillis() + config.inputTimeoutSeconds * MILLIS_PER_SECOND;
        pendingPng.put(userId, new PendingPng(account, slot, expiresAt));
        channel.sendMessageEmbeds(embeds.embed(EmbedType.INFO, botMessages.skins.bot.pngPrompt
                        .replace("{slot}", String.valueOf(slot))
                        .replace("{seconds}", String.valueOf(config.inputTimeoutSeconds))))
                .queue();
    }

    public void handleButton(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        String action = event.getComponentId().substring(PREFIX.length());
        if (action.equals(ACTION_OPEN)) {
            open(event, userId);
        } else if (action.startsWith(ACTION_ACCOUNT)) {
            withAccount(event, userId, action.substring(ACTION_ACCOUNT.length()), account -> hub(event, account));
        } else if (action.startsWith(ACTION_VIEW)) {
            withSlot(event, userId, action.substring(ACTION_VIEW.length()), (account, slot) -> view(event, account, slot));
        } else if (action.startsWith(ACTION_EDIT)) {
            withSlot(event, userId, action.substring(ACTION_EDIT.length()), (account, slot) -> chooseType(event, account, slot));
        } else if (action.startsWith(ACTION_TYPE)) {
            String rest = action.substring(ACTION_TYPE.length());
            int split = rest.lastIndexOf('_');
            if (split > 0) {
                String kind = rest.substring(split + 1);
                withSlot(event, userId, rest.substring(0, split),
                        (account, slot) -> chooseSource(event, account, slot, kind));
            }
        } else if (action.startsWith(ACTION_DELETE_YES)) {
            withSlot(event, userId, action.substring(ACTION_DELETE_YES.length()),
                    (account, slot) -> acknowledge(event, hook -> confirmDelete(hook, account, slot)));
        } else if (action.startsWith(ACTION_DELETE_NO)) {
            withSlot(event, userId, action.substring(ACTION_DELETE_NO.length()), (account, slot) -> view(event, account, slot));
        } else if (action.startsWith(ACTION_DELETE)) {
            withSlot(event, userId, action.substring(ACTION_DELETE.length()), (account, slot) -> askDelete(event, account, slot));
        } else {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, botMessages.skins.bot.notBound)).queue();
        }
    }

    public void handleModal(ModalInteractionEvent event) {
        String userId = event.getUser().getId();
        UUID owner = bindingService.getPlayerByBinding(BindingType.DISCORD, userId);
        if (owner == null) {
            event.reply(botMessages.skins.bot.notBound).setEphemeral(true).queue();
            return;
        }
        String rest = event.getModalId().substring(MODAL_PREFIX.length());
        int kindSplit = rest.lastIndexOf('_');
        String target = rest.substring(0, Math.max(kindSplit, 0));
        boolean nick = rest.substring(kindSplit + 1).equals(KIND_NICK);
        int slotSplit = target.lastIndexOf('_');
        SkinService.Account account = skins.account(owner, target.substring(0, Math.max(slotSplit, 0)));
        int slot = parseSlot(target.substring(slotSplit + 1));
        if (account == null || slot < 1 || slot > profileCount()) {
            event.reply(botMessages.skins.bot.accountNotFound).setEphemeral(true).queue();
            return;
        }
        String value = event.getValue(MODAL_FIELD) != null ? event.getValue(MODAL_FIELD).getAsString() : "";
        UUID uuid = account.uuid();
        event.deferReply().queue(hook -> CompletableFuture
                .supplyAsync(() -> nick ? skins.setFromNick(uuid, slot, value) : skins.setFromUrl(uuid, slot, value))
                .thenAccept(result -> sendSaveResult(hook, result.status(), account, slot, nick, value)));
    }

    public void handlePrivateMessage(MessageReceivedEvent event) {
        String userId = event.getAuthor().getId();
        PendingPng pending = pendingPng.get(userId);
        if (pending == null) {
            return;
        }
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        if (attachments.isEmpty()) {
            event.getChannel().sendMessageEmbeds(embeds.embed(EmbedType.INFO, botMessages.skins.bot.pngWaitHint)).queue();
            return;
        }
        pendingPng.remove(userId);
        Message.Attachment attachment = attachments.get(0);
        if (!attachment.isImage()) {
            event.getChannel().sendMessageEmbeds(embeds.embed(EmbedType.ERROR, botMessages.skins.bot.invalidPng))
                    .addActionRow(hubButton(pending.account()), openMenuButton()).queue();
            return;
        }
        if (attachment.getSize() > config.maxPngBytes) {
            event.getChannel().sendMessageEmbeds(embeds.embed(EmbedType.ERROR,
                            botMessages.skins.bot.tooLarge.replace("{max}", String.valueOf(config.maxPngBytes / 1024))))
                    .addActionRow(hubButton(pending.account()), openMenuButton()).queue();
            return;
        }
        CompletableFuture
                .supplyAsync(() -> download(attachment))
                .thenApply(bytes -> bytes == null
                        ? new SkinService.Result(SkinService.Status.ERROR, null)
                        : skins.setFromPng(pending.account(), pending.slot(), bytes))
                .thenAccept(result -> {
                    MessageEmbed embed = resultEmbed(result.status(), pending.slot(), KIND_PNG, null);
                    event.getChannel().sendMessageEmbeds(embed)
                            .addActionRow(hubButton(pending.account()), openMenuButton()).queue();
                });
    }

    private void withAccount(ButtonInteractionEvent event, String userId, String rawUuid, AccountAction action) {
        UUID owner = boundPlayer(event, userId);
        if (owner == null) {
            return;
        }
        SkinService.Account account = skins.account(owner, rawUuid);
        if (account == null) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, botMessages.skins.bot.accountNotFound)).queue();
            return;
        }
        action.run(account);
    }

    private void withSlot(ButtonInteractionEvent event, String userId, String target, SlotAction action) {
        UUID owner = boundPlayer(event, userId);
        if (owner == null) {
            return;
        }
        int split = target.lastIndexOf('_');
        SkinService.Account account = split > 0 ? skins.account(owner, target.substring(0, split)) : null;
        int slot = split > 0 ? parseSlot(target.substring(split + 1)) : -1;
        if (account == null) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, botMessages.skins.bot.accountNotFound)).queue();
            return;
        }
        if (slot < 1 || slot > profileCount()) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, botMessages.skins.bot.notFound.replace("{input}", target))).queue();
            return;
        }
        action.run(account.uuid(), slot);
    }

    private void hub(ButtonInteractionEvent event, SkinService.Account account) {
        event.editMessageEmbeds(embeds.embed(EmbedType.INFO, botMessages.skins.bot.hubTitle.replace("{account}", account.name())))
                .setComponents(hubRows(account))
                .queue();
    }

    private void view(ButtonInteractionEvent event, UUID account, int slot) {
        SkinProfile profile = skins.profile(account, slot);
        String title = botMessages.skins.bot.profileTitle.replace("{slot}", String.valueOf(slot));
        if (profile == null) {
            event.editMessageEmbeds(embeds.embed(EmbedType.INFO, title + "\n\n" + botMessages.skins.bot.profileEmpty))
                    .setComponents(ActionRow.of(
                            button(botMessages.skins.discordButtons.action, targetId(ACTION_EDIT, account, slot), botMessages.skins.bot.setButton),
                            hubButton(account)))
                    .queue();
            return;
        }
        MessageEmbed base = embeds.embed(EmbedType.INFO, title + "\n\n" + botMessages.skins.bot.profileFilled
                .replace("{input}", profile.input())
                .replace("{source}", sourceLabel(profile)));
        String texture = profile.textureUrl();
        MessageEmbed embed = texture != null ? new EmbedBuilder(base).setImage(texture).build() : base;
        event.editMessageEmbeds(embed)
                .setComponents(rows(List.of(
                        button(botMessages.skins.discordButtons.action, targetId(ACTION_EDIT, account, slot), botMessages.skins.bot.editButton),
                        button(botMessages.skins.discordButtons.delete, targetId(ACTION_DELETE, account, slot), botMessages.skins.bot.deleteButton),
                        hubButton(account))))
                .queue();
    }

    private void chooseType(ButtonInteractionEvent event, UUID account, int slot) {
        String title = botMessages.skins.bot.typeTitle.replace("{slot}", String.valueOf(slot));
        event.editMessageEmbeds(embeds.embed(EmbedType.INFO, title))
                .setComponents(rows(List.of(
                        button(botMessages.skins.discordButtons.action, targetId(ACTION_TYPE, account, slot) + "_" + KIND_NICK,
                                botMessages.skins.bot.typeNickButton),
                        button(botMessages.skins.discordButtons.action, targetId(ACTION_TYPE, account, slot) + "_" + KIND_URL,
                                botMessages.skins.bot.typeUrlButton),
                        button(botMessages.skins.discordButtons.action, targetId(ACTION_TYPE, account, slot) + "_" + KIND_PNG,
                                botMessages.skins.bot.typePngButton),
                        button(botMessages.skins.discordButtons.back, targetId(ACTION_VIEW, account, slot), botMessages.skins.bot.backButton))))
                .queue();
    }

    private void chooseSource(ButtonInteractionEvent event, UUID account, int slot, String kind) {
        String target = account + "_" + slot;
        switch (kind) {
            case KIND_NICK -> event.replyModal(Modal.create(MODAL_PREFIX + target + "_" + KIND_NICK, botMessages.skins.bot.nickModalTitle)
                    .addActionRow(TextInput.create(MODAL_FIELD, botMessages.skins.bot.nickModalLabel, TextInputStyle.SHORT)
                            .setPlaceholder(botMessages.skins.bot.nickModalPlaceholder)
                            .setMinLength(MIN_NICK_LENGTH)
                            .setMaxLength(MAX_NICK_LENGTH)
                            .setRequired(true)
                            .build())
                    .build()).queue();
            case KIND_URL -> event.replyModal(Modal.create(MODAL_PREFIX + target + "_" + KIND_URL, botMessages.skins.bot.urlModalTitle)
                    .addActionRow(TextInput.create(MODAL_FIELD, botMessages.skins.bot.urlModalLabel, TextInputStyle.SHORT)
                            .setPlaceholder(botMessages.skins.bot.urlModalPlaceholder)
                            .setMinLength(1)
                            .setMaxLength(MAX_URL_LENGTH)
                            .setRequired(true)
                            .build())
                    .build()).queue();
            case KIND_PNG -> {
                long expiresAt = System.currentTimeMillis() + config.inputTimeoutSeconds * MILLIS_PER_SECOND;
                pendingPng.put(event.getUser().getId(), new PendingPng(account, slot, expiresAt));
                event.editMessageEmbeds(embeds.embed(EmbedType.INFO, botMessages.skins.bot.pngPrompt
                                .replace("{slot}", String.valueOf(slot))
                                .replace("{seconds}", String.valueOf(config.inputTimeoutSeconds))))
                        .setComponents(ActionRow.of(button(botMessages.skins.discordButtons.back, targetId(ACTION_VIEW, account, slot),
                                botMessages.skins.bot.backButton)))
                        .queue();
            }
            default -> event.replyEmbeds(embeds.embed(EmbedType.ERROR, botMessages.skins.bot.notFound)).queue();
        }
    }

    private void askDelete(ButtonInteractionEvent event, UUID account, int slot) {
        event.editMessageEmbeds(embeds.embed(EmbedType.WARNING, botMessages.skins.bot.deleteConfirm.replace("{slot}", String.valueOf(slot))))
                .setComponents(ActionRow.of(
                        button(botMessages.skins.discordButtons.confirm, targetId(ACTION_DELETE_YES, account, slot), botMessages.skins.bot.deleteYesButton),
                        button(botMessages.skins.discordButtons.back, targetId(ACTION_DELETE_NO, account, slot), botMessages.skins.bot.deleteNoButton)))
                .queue();
    }

    private void confirmDelete(InteractionHook hook, UUID account, int slot) {
        skins.remove(account, slot);
        hook.sendMessageEmbeds(embeds.embed(EmbedType.SUCCESS, botMessages.skins.bot.deleted.replace("{slot}", String.valueOf(slot))))
                .addActionRow(hubButton(account), openMenuButton())
                .queue();
    }

    private void sendSaveResult(InteractionHook hook, SkinService.Status status, SkinService.Account account, int slot,
                                boolean nick, String value) {
        MessageEmbed embed = resultEmbed(status, slot, nick ? KIND_NICK : KIND_URL, value);
        hook.sendMessageEmbeds(embed).addActionRow(hubButton(account.uuid()), openMenuButton()).queue();
    }

    private MessageEmbed resultEmbed(SkinService.Status status, int slot, String kind, String value) {
        String slotText = String.valueOf(slot);
        return switch (status) {
            case OK -> embeds.embed(EmbedType.SUCCESS, botMessages.skins.bot.saved.replace("{slot}", slotText));
            case INVALID_INPUT -> embeds.embed(EmbedType.ERROR,
                    kind.equals(KIND_NICK) ? botMessages.skins.bot.invalidNick : botMessages.skins.bot.invalidUrl);
            case NOT_FOUND -> embeds.embed(EmbedType.ERROR, botMessages.skins.bot.notFound.replace("{input}", value));
            case UNAVAILABLE -> embeds.embed(EmbedType.ERROR, botMessages.skins.bot.unavailable);
            case INVALID_PNG -> embeds.embed(EmbedType.ERROR, botMessages.skins.bot.invalidPng);
            case TOO_LARGE -> embeds.embed(EmbedType.ERROR,
                    botMessages.skins.bot.tooLarge.replace("{max}", String.valueOf(config.maxPngBytes / 1024)));
            case ERROR -> embeds.embed(EmbedType.ERROR, botMessages.skins.bot.saveError);
        };
    }

    private byte[] download(Message.Attachment attachment) {
        try (InputStream in = attachment.getProxy().download().get(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            return in.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    private void acknowledge(ButtonInteractionEvent event, Consumer<InteractionHook> then) {
        event.editComponents(Collections.emptyList()).queue(then);
    }

    private UUID boundPlayer(ButtonInteractionEvent event, String userId) {
        UUID owner = bindingService.getPlayerByBinding(BindingType.DISCORD, userId);
        if (owner == null) {
            event.replyEmbeds(embeds.embed(EmbedType.ERROR, botMessages.skins.bot.notBound)).queue();
        }
        return owner;
    }

    private List<ActionRow> accountRows(UUID owner) {
        List<Button> buttons = new ArrayList<>();
        for (SkinService.Account account : skins.accounts(owner)) {
            buttons.add(button(botMessages.skins.discordButtons.profile, PREFIX + ACTION_ACCOUNT + account.uuid(),
                    shorten(account.name())));
        }
        buttons.add(button(botMessages.skins.discordButtons.back, MENU_BACK, botMessages.skins.bot.backButton));
        return rows(buttons, botMessages.skins.layout.discordSkinsAccountsPerRow);
    }

    private List<ActionRow> hubRows(SkinService.Account account) {
        List<Button> buttons = new ArrayList<>();
        for (int slot = 1; slot <= profileCount(); slot++) {
            SkinProfile profile = skins.profile(account.uuid(), slot);
            String name = profile == null ? config.labels.emptyName : sourceName(profile);
            String label = botMessages.skins.bot.hubProfileButton
                    .replace("{slot}", String.valueOf(slot))
                    .replace("{name}", shorten(name));
            buttons.add(button(botMessages.skins.discordButtons.profile, targetId(ACTION_VIEW, account.uuid(), slot), label));
        }
        buttons.add(button(botMessages.skins.discordButtons.back, PREFIX + ACTION_OPEN, botMessages.skins.bot.backButton));
        return rows(buttons, botMessages.skins.layout.discordSkinsProfilesPerRow);
    }

    private String targetId(String action, UUID account, int slot) {
        return PREFIX + action + account + "_" + slot;
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

    private Button hubButton(UUID account) {
        return button(botMessages.skins.discordButtons.back, PREFIX + ACTION_ACCOUNT + account, botMessages.skins.bot.hubButton);
    }

    private Button openMenuButton() {
        return button(botMessages.skins.discordButtons.menu, MENU_OPEN, botMessages.skins.bot.openMenuButton);
    }

    private Button button(String style, String componentId, String label) {
        return Button.of(parseStyle(style), componentId, label);
    }

    private static ButtonStyle parseStyle(String name) {
        if (name != null) {
            try {
                ButtonStyle style = ButtonStyle.valueOf(name.trim().toUpperCase(Locale.ROOT));
                if (style != ButtonStyle.LINK && style != ButtonStyle.UNKNOWN) {
                    return style;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ButtonStyle.SECONDARY;
    }

    @FunctionalInterface
    private interface AccountAction {
        void run(SkinService.Account account);
    }

    @FunctionalInterface
    private interface SlotAction {
        void run(UUID account, int slot);
    }
}
