package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.util.Colors;

public class DiscordBotMessages {

    public enum EmbedType { INFO, SUCCESS, ERROR, WARNING }

    public enum ButtonKey {
        MENU_MANAGE, MENU_UNBIND, MENU_PASSWORD, MENU_SYNC, MENU_TWINK, MENU_BACK, MENU_OPEN, MENU_TWINK_ADD, MENU_TWINK_DELETE, MENU_TWINK_DELETE_YES, MENU_TWINK_DELETE_NO,
        MENU_UNBIND_YES, MENU_UNBIND_NO, MENU_PASSWORD_YES, MENU_PASSWORD_NO,
        LOGIN_CONFIRM, LOGIN_DENY
    }

    private final Config config;

    public DiscordBotMessages(JavaPlugin plugin) {
        this.config = ConfigLoader.load(plugin, Config.class, "bots/messages.yml");
    }

    public void reload() {
        try {
            config.load(true);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось перезагрузить конфиг сообщений Discord-бота", e);
        }
    }

    public String getDiscordAlreadyBound(String playerName) {
        return format(config.discord.alreadyBound, playerName);
    }

    public String getDiscordInstructions(boolean velocity) {
        return Colors.convertColorCodes(velocity ? config.discord.instructionsVelocity : config.discord.instructions);
    }

    public String getDiscordCodeUsed(String playerName) {
        return format(config.discord.codeUsed, playerName);
    }

    public String getDiscordBindingSuccess(String playerName) {
        return format(config.discord.bindingSuccess, playerName);
    }

    public String getDiscordBindingError() {
        return Colors.convertColorCodes(config.discord.bindingError);
    }

    public String getDiscordInvalidCode() {
        return Colors.convertColorCodes(config.discord.invalidCode);
    }

    public String getDiscordTooManyAttempts(long minutes) {
        return Colors.convertColorCodes(config.discord.tooManyAttempts.replace("{minutes}", String.valueOf(minutes)));
    }

    public String getPresence(int online) {
        return config.discord.presence.replace("{online}", String.valueOf(online));
    }

    public String getLoginConfirmTitle() {
        return config.discord.loginConfirmTitle;
    }

    public String getLoginConfirmDescription(String playerName, int timeoutSeconds) {
        return config.discord.loginConfirmDescription
                .replace("{player}", safeName(playerName))
                .replace("{seconds}", String.valueOf(timeoutSeconds));
    }

    public String getLoginConfirmButton() {
        return config.discord.loginConfirmButton;
    }

    public String getLoginDenyButton() {
        return config.discord.loginDenyButton;
    }

    public String getLoginConfirmed() {
        return config.discord.loginConfirmed;
    }

    public String getLoginDenied() {
        return config.discord.loginDenied;
    }

    public String getMenuCommandDescription() {
        return config.discord.menuCommandDescription;
    }

    public String getMenuTitle() {
        return config.discord.menuTitle;
    }

    public String getMenuManageButton() {
        return config.discord.menuManageButton;
    }

    public String getMenuManageTitle() {
        return config.discord.menuManageTitle;
    }

    public String getMenuUnbindButton() {
        return config.discord.menuUnbindButton;
    }

    public String getMenuPasswordButton() {
        return config.discord.menuPasswordButton;
    }

    public String getMenuSyncButton() {
        return config.discord.menuSyncButton;
    }

    public String getMenuSyncSuccess(String playerName) {
        return format(config.discord.menuSyncSuccess, playerName);
    }

    public String getMenuSyncUnavailable() {
        return config.discord.menuSyncUnavailable;
    }

    public String getMenuTwinkButton() {
        return config.discord.menuTwinkButton;
    }

    public String getMenuUnbindYesButton() {
        return config.discord.menuUnbindYesButton;
    }

    public String getMenuUnbindNoButton() {
        return config.discord.menuUnbindNoButton;
    }

    public String getMenuPasswordYesButton() {
        return config.discord.menuPasswordYesButton;
    }

    public String getMenuPasswordNoButton() {
        return config.discord.menuPasswordNoButton;
    }

    public String getMenuNotBound() {
        return config.discord.menuNotBound;
    }

    public String getMenuUnbindError() {
        return config.discord.menuUnbindError;
    }

    public String getMenuUnbindCancelled() {
        return config.discord.menuUnbindCancelled;
    }

    public String getMenuPasswordError() {
        return config.discord.menuPasswordError;
    }

    public String getMenuPasswordUnavailable() {
        return config.discord.menuPasswordUnavailable;
    }

    public String getMenuPasswordCancelled() {
        return config.discord.menuPasswordCancelled;
    }

    public String getMenuUnknownAction() {
        return config.discord.menuUnknownAction;
    }

    public String getMenuUnbindConfirm(String playerName) {
        return config.discord.menuUnbindConfirm.replace("{player}", safeName(playerName));
    }

    public String getMenuUnbindSuccess(String playerName) {
        return config.discord.menuUnbindSuccess.replace("{player}", safeName(playerName));
    }

    public String getMenuPasswordConfirm(String playerName) {
        return config.discord.menuPasswordConfirm.replace("{player}", safeName(playerName));
    }

    public String getMenuPasswordSuccess(String playerName, String password) {
        return config.discord.menuPasswordSuccess
                .replace("{player}", safeName(playerName))
                .replace("{password}", password);
    }

    public String getMenuPasswordNotRegistered(String playerName) {
        return config.discord.menuPasswordNotRegistered.replace("{player}", safeName(playerName));
    }

    public String getMenuOpenButton() {
        return config.discord.menuOpenButton;
    }

    public String getTwinkListItem(String name, boolean pending) {
        return config.discord.menuTwinkListItem
                .replace("{twink}", name)
                .replace("{status}", pending ? config.discord.menuTwinkStatusPending : config.discord.menuTwinkStatusBound);
    }

    public String getTwinkListEmpty() {
        return config.discord.menuTwinkListEmpty;
    }

    public String getTwinkLimit(int max, String list) {
        return config.discord.menuTwinkLimit.replace("{max}", String.valueOf(max)).replace("{list}", list);
    }

    public String getTwinkCreated(String twink, String code, int minutes) {
        return config.discord.menuTwinkCreated
                .replace("{twink}", twink)
                .replace("{code}", code)
                .replace("{minutes}", String.valueOf(minutes));
    }

    public String getTwinkInvalidName() {
        return config.discord.menuTwinkInvalidName;
    }

    public String getTwinkAlreadyBound(String twink) {
        return config.discord.menuTwinkAlreadyBound.replace("{twink}", twink);
    }

    public String getTwinkPending(String twink) {
        return config.discord.menuTwinkPending.replace("{twink}", twink);
    }

    public String getTwinkNotSeen(String twink) {
        return config.discord.menuTwinkNotSeen.replace("{twink}", twink);
    }

    public String getTwinkUnavailable() {
        return config.discord.menuTwinkUnavailable;
    }

    public String getTwinkModalTitle() {
        return config.discord.menuTwinkModalTitle;
    }

    public String getTwinkModalLabel() {
        return config.discord.menuTwinkModalLabel;
    }

    public String getTwinkModalPlaceholder() {
        return config.discord.menuTwinkModalPlaceholder;
    }

    public String getTwinksTitle(int free, int max, String list) {
        return config.discord.menuTwinksTitle
                .replace("{free}", String.valueOf(free))
                .replace("{max}", String.valueOf(max))
                .replace("{list}", list);
    }

    public String getTwinkAddButton() {
        return config.discord.menuTwinkAddButton;
    }

    public String getTwinkDeleteButton(String twink) {
        return config.discord.menuTwinkDeleteButton.replace("{twink}", twink);
    }

    public String getTwinkDeleteConfirm(String twink) {
        return config.discord.menuTwinkDeleteConfirm.replace("{twink}", twink);
    }

    public String getTwinkDeleteYesButton() {
        return config.discord.menuTwinkDeleteYesButton;
    }

    public String getTwinkDeleteNoButton() {
        return config.discord.menuTwinkDeleteNoButton;
    }

    public String getTwinkDeleted(String twink) {
        return config.discord.menuTwinkDeleted.replace("{twink}", twink);
    }

    public String getTwinkDeleteCancelled() {
        return config.discord.menuTwinkDeleteCancelled;
    }

    public String getTwinkDeleteNotFound(String twink) {
        return config.discord.menuTwinkDeleteNotFound.replace("{twink}", twink);
    }

    public String getMenuBackButton() {
        return config.discord.menuBackButton;
    }

    public String getEmbedColor(EmbedType type) {
        Config.EmbedColors colors = config.discord.colors;
        return switch (type) {
            case INFO -> colors.info;
            case SUCCESS -> colors.success;
            case ERROR -> colors.error;
            case WARNING -> colors.warning;
        };
    }

    public String getButtonStyle(ButtonKey key) {
        Config.ButtonStyles styles = config.discord.buttonStyles;
        return switch (key) {
            case MENU_MANAGE -> styles.menuManage;
            case MENU_UNBIND -> styles.menuUnbind;
            case MENU_PASSWORD -> styles.menuPassword;
            case MENU_SYNC -> styles.menuSync;
            case MENU_TWINK -> styles.menuTwink;
            case MENU_BACK -> styles.menuBack;
            case MENU_OPEN -> styles.menuOpen;
            case MENU_TWINK_ADD -> styles.menuTwinkAdd;
            case MENU_TWINK_DELETE -> styles.menuTwinkDelete;
            case MENU_TWINK_DELETE_YES -> styles.menuTwinkDeleteYes;
            case MENU_TWINK_DELETE_NO -> styles.menuTwinkDeleteNo;
            case MENU_UNBIND_YES -> styles.menuUnbindYes;
            case MENU_UNBIND_NO -> styles.menuUnbindNo;
            case MENU_PASSWORD_YES -> styles.menuPasswordYes;
            case MENU_PASSWORD_NO -> styles.menuPasswordNo;
            case LOGIN_CONFIRM -> styles.loginConfirm;
            case LOGIN_DENY -> styles.loginDeny;
        };
    }

    private String format(String text, String playerName) {
        return Colors.convertColorCodes(text.replace("{player}", safeName(playerName)));
    }

    private String safeName(String playerName) {
        return playerName == null || playerName.isBlank() ? "неизвестно" : playerName;
    }

    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class Config extends OkaeriConfig {

        public Discord discord = new Discord();

        public static class Discord extends OkaeriConfig {
            public String alreadyBound = "✅ Ваш Discord аккаунт уже привязан к игроку: **{player}**";

            public String instructions = "**Привязка аккаунта дискорда к майну**\n\n" +
                    "1. В игре выполните команду ```/binding code```\n" +
                    "2. Скопируйте полученный код (6 символов)\n" +
                    "3. Отправьте этот код боту в личку\n\n" +
                    "После этого ваш аккаунт будет привязан!";

            public String instructionsVelocity = "**Привязка аккаунта дискорда к майну**\n\n" +
                    "1. Зайдите на сервер, код привязки (6 символов) будет выдан сразу при входе\n" +
                    "2. Скопируйте полученный код\n" +
                    "3. Отправьте этот код боту в личку\n\n" +
                    "После этого ваш аккаунт будет привязан!";

            public String codeUsed = "❌ Этот код уже использован. Аккаунт игрока **{player}** уже привязан.";

            public String bindingSuccess = "✅ Аккаунт успешно привязан к игроку: **{player}**\nТеперь вы можете играть на сервере!";

            public String bindingError = "❌ Ошибка при привязке аккаунта. Попробуйте позже.";

            public String invalidCode = "❌ Неверный или просроченный код. Получите новый командой /binding code в игре.";

            public String tooManyAttempts = "⏳ Слишком много неверных кодов. Попробуйте снова через {minutes} мин.";

            public String presence = "Онлайн: {online}";

            public String loginConfirmTitle = "🔐 Подтверждение входа на сервер";

            public String loginConfirmDescription = "Игрок **{player}** заходит на сервер.\n" +
                    "Если это вы - подтвердите вход в течение **{seconds} секунд**, иначе вы будете кикнуты.";

            public String loginConfirmButton = "✅ Подтвердить вход";

            public String loginDenyButton = "❌ Это не я";

            public String loginConfirmed = "✅ Вход подтверждён.";

            public String loginDenied = "❌ Вход отклонён, игрок кикнут с сервера.";

            public String menuCommandDescription = "Открыть меню";

            public String menuTitle = "**📋 Меню**\n\nВыберите действие:";

            public String menuManageButton = "⚙️ Управление аккаунтом";

            public String menuManageTitle = "**⚙️ Управление аккаунтом**\n\nВыберите действие:";

            public String menuUnbindButton = "🔓 Отвязать аккаунт";

            public String menuPasswordButton = "🔑 Сменить пароль";

            public String menuTwinkButton = "👥 Твинки";

            public String menuSyncButton = "🔄 Синхронизировать ник и роль";

            public String menuSyncSuccess = "✅ Ник и роль синхронизированы с игроком **{player}**.";

            public String menuSyncUnavailable = "❌ Не удалось синхронизировать: проверьте, что вы находитесь на Discord сервере, а выдача ролей и смена ника включены.";

            public String menuNotBound = "❌ Ваш Discord аккаунт не привязан к игроку. Отправьте /start, чтобы узнать, как его привязать.";

            public String menuUnbindConfirm = "⚠️ Вы действительно хотите отвязать Discord от игрока **{player}**?";

            public String menuUnbindYesButton = "✅ Да, отвязать";

            public String menuUnbindNoButton = "❌ Отмена";

            public String menuUnbindSuccess = "✅ Discord успешно отвязан от игрока **{player}**.";

            public String menuUnbindError = "❌ Не удалось отвязать аккаунт. Попробуйте позже.";

            public String menuUnbindCancelled = "Отвязка отменена.";

            public String menuPasswordConfirm = "⚠️ Сменить пароль игрока **{player}**? Старый пароль перестанет работать.";

            public String menuPasswordYesButton = "✅ Да, сменить";

            public String menuPasswordNoButton = "❌ Отмена";

            public String menuPasswordSuccess = "✅ Новый пароль игрока **{player}**:\n```{password}```\nСмените его в игре командой /changepassword.";

            public String menuPasswordError = "❌ Не удалось сменить пароль. Попробуйте позже.";

            public String menuPasswordUnavailable = "❌ Смена пароля сейчас недоступна.";

            public String menuPasswordNotRegistered = "❌ Игрок **{player}** не зарегистрирован в системе авторизации.";

            public String menuPasswordCancelled = "Смена пароля отменена.";

            public String menuUnknownAction = "❓ Неизвестное действие.";

            public String menuBackButton = "⬅️ Назад";

            public String menuOpenButton = "📋 В меню";

            public String menuTwinksTitle = "**👥 Твинки**\n\nСвободно мест: **{free}** из **{max}**.\n\n{list}";

            public String menuTwinkAddButton = "➕ Добавить твинк";

            public String menuTwinkDeleteButton = "🗑 {twink}";

            public String menuTwinkDeleteConfirm = "⚠️ Удалить твинка **{twink}**? Он потеряет доступ к серверу и будет кикнут.";

            public String menuTwinkDeleteYesButton = "✅ Да, удалить";

            public String menuTwinkDeleteNoButton = "❌ Отмена";

            public String menuTwinkDeleted = "✅ Твинк **{twink}** удалён.";

            public String menuTwinkDeleteCancelled = "Удаление отменено.";

            public String menuTwinkDeleteNotFound = "❌ Твинк **{twink}** не найден.";
            public String menuTwinkModalTitle = "Добавление твинка";

            public String menuTwinkModalLabel = "Ник твинка";

            public String menuTwinkModalPlaceholder = "Например, Steve_2";

            public String menuTwinkListItem = "• **{twink}** — {status}";

            public String menuTwinkStatusBound = "привязан";

            public String menuTwinkStatusPending = "ждёт код";

            public String menuTwinkListEmpty = "Твинков пока нет.";

            public String menuTwinkLimit = "❌ Достигнут лимит твинков: **{max}**.\n\n{list}";

            public String menuTwinkCreated = "✅ Код для твинка **{twink}**:\n```{code}```\nЗайдите твинком на сервер и отправьте этот код в чат в течение **{minutes} мин.** После этого твинк сможет играть, а подтверждения входа будут приходить сюда.";

            public String menuTwinkInvalidName = "❌ Ник должен состоять из 3-16 латинских букв, цифр или знаков подчёркивания.";

            public String menuTwinkAlreadyBound = "❌ Ник **{twink}** уже привязан к аккаунту.";

            public String menuTwinkPending = "❌ Для ника **{twink}** уже ожидается код привязки.";

            public String menuTwinkNotSeen = "❌ Игрок **{twink}** ещё не заходил на прокси-сервер. Зайдите твинком на сервер, зарегистрируйтесь и повторите.";

            public String menuTwinkUnavailable = "❌ Добавление твинков сейчас недоступно.";


            @Comment("Цвета embed-сообщений в формате #RRGGBB")
            public EmbedColors colors = new EmbedColors();

            @Comment("Цвета кнопок: PRIMARY (синий), SECONDARY (серый), SUCCESS (зелёный), DANGER (красный)")
            public ButtonStyles buttonStyles = new ButtonStyles();
        }

        public static class EmbedColors extends OkaeriConfig {
            @Comment("Обычные сообщения: меню, инструкции")
            public String info = "#5865F2";
            @Comment("Успешные действия")
            public String success = "#57F287";
            @Comment("Ошибки и отказы")
            public String error = "#ED4245";
            @Comment("Предупреждения: подтверждения, блокировка попыток, запрос входа")
            public String warning = "#FFA500";
        }

        public static class ButtonStyles extends OkaeriConfig {
            public String menuManage = "PRIMARY";
            public String menuUnbind = "DANGER";
            public String menuPassword = "SECONDARY";
            public String menuSync = "SUCCESS";
            public String menuTwink = "SECONDARY";
            public String menuBack = "SECONDARY";
            public String menuOpen = "PRIMARY";
            public String menuTwinkAdd = "SUCCESS";
            public String menuTwinkDelete = "DANGER";
            public String menuTwinkDeleteYes = "DANGER";
            public String menuTwinkDeleteNo = "SECONDARY";
            public String menuUnbindYes = "DANGER";
            public String menuUnbindNo = "SECONDARY";
            public String menuPasswordYes = "SUCCESS";
            public String menuPasswordNo = "SECONDARY";
            public String loginConfirm = "SUCCESS";
            public String loginDeny = "DANGER";
        }
    }
}
