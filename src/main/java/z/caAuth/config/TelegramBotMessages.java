package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.bots.telegram.TelegramTextFormatter;
import z.caAuth.util.Colors;

public class TelegramBotMessages {

    private final Config config;

    public TelegramBotMessages(JavaPlugin plugin) {
        this.config = ConfigLoader.load(plugin, Config.class, "bots/messages.yml");
    }

    public void reload() {
        try {
            config.load(true);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось перезагрузить конфиг сообщений Telegram-бота", e);
        }
    }

    public String getTelegramAlreadyBound(String playerName) {
        return format(config.telegram.alreadyBound, playerName);
    }

    public String getTelegramInstructions(boolean velocity) {
        return Colors.convertColorCodes(velocity ? config.telegram.instructionsVelocity : config.telegram.instructions);
    }

    public String getTelegramCodeUsed(String playerName) {
        return format(config.telegram.codeUsed, playerName);
    }

    public String getTelegramBindingSuccess(String playerName) {
        return format(config.telegram.bindingSuccess, playerName);
    }

    public String getTelegramBindingError() {
        return Colors.convertColorCodes(config.telegram.bindingError);
    }

    public String getTelegramInvalidCode() {
        return Colors.convertColorCodes(config.telegram.invalidCode);
    }

    public String getTelegramTooManyAttempts(long minutes) {
        return Colors.convertColorCodes(config.telegram.tooManyAttempts.replace("{minutes}", String.valueOf(minutes)));
    }

    public String getTelegramMenuTitle() {
        return Colors.convertColorCodes(config.telegram.menuTitle);
    }

    public String getTelegramMenuNotBound() {
        return Colors.convertColorCodes(config.telegram.menuNotBound);
    }

    public String getTelegramMenuUnbindConfirm(String playerName) {
        return format(config.telegram.menuUnbindConfirm, playerName);
    }

    public String getTelegramMenuUnbindSuccess(String playerName) {
        return format(config.telegram.menuUnbindSuccess, playerName);
    }

    public String getTelegramMenuUnbindError() {
        return Colors.convertColorCodes(config.telegram.menuUnbindError);
    }

    public String getTelegramMenuUnbindCancelled() {
        return Colors.convertColorCodes(config.telegram.menuUnbindCancelled);
    }

    public String getMenuOpenButton() {
        return config.telegram.menuOpenButton;
    }

    public String getTwinkListItem(String name, boolean pending) {
        return config.telegram.menuTwinkListItem
                .replace("{twink}", name)
                .replace("{status}", pending ? config.telegram.menuTwinkStatusPending : config.telegram.menuTwinkStatusBound);
    }

    public String getTwinkListEmpty() {
        return config.telegram.menuTwinkListEmpty;
    }

    public String getTwinkLimit(int max, String list) {
        return config.telegram.menuTwinkLimit.replace("{max}", String.valueOf(max)).replace("{list}", list);
    }

    public String getTwinkCreated(String twink, String code, int minutes) {
        return config.telegram.menuTwinkCreated
                .replace("{twink}", twink)
                .replace("{code}", code)
                .replace("{minutes}", String.valueOf(minutes));
    }

    public String getTwinkInvalidName() {
        return config.telegram.menuTwinkInvalidName;
    }

    public String getTwinkAlreadyBound(String twink) {
        return config.telegram.menuTwinkAlreadyBound.replace("{twink}", twink);
    }

    public String getTwinkPending(String twink) {
        return config.telegram.menuTwinkPending.replace("{twink}", twink);
    }

    public String getTwinkNotSeen(String twink) {
        return config.telegram.menuTwinkNotSeen.replace("{twink}", twink);
    }

    public String getTwinkUnavailable() {
        return config.telegram.menuTwinkUnavailable;
    }

    public String getTwinkPrompt(int free, int max, String list) {
        return config.telegram.menuTwinkPrompt
                .replace("{free}", String.valueOf(free))
                .replace("{max}", String.valueOf(max))
                .replace("{list}", list);
    }

    public String getTwinksTitle(int free, int max, String list) {
        return config.telegram.menuTwinksTitle
                .replace("{free}", String.valueOf(free))
                .replace("{max}", String.valueOf(max))
                .replace("{list}", list);
    }

    public String getTwinkAddButton() {
        return config.telegram.menuTwinkAddButton;
    }

    public String getTwinkDeleteButton(String twink) {
        return config.telegram.menuTwinkDeleteButton.replace("{twink}", twink);
    }

    public String getTwinkDeleteConfirm(String twink) {
        return config.telegram.menuTwinkDeleteConfirm.replace("{twink}", twink);
    }

    public String getTwinkDeleteYesButton() {
        return config.telegram.menuTwinkDeleteYesButton;
    }

    public String getTwinkDeleteNoButton() {
        return config.telegram.menuTwinkDeleteNoButton;
    }

    public String getTwinkDeleted(String twink) {
        return config.telegram.menuTwinkDeleted.replace("{twink}", twink);
    }

    public String getTwinkDeleteCancelled() {
        return config.telegram.menuTwinkDeleteCancelled;
    }

    public String getTwinkDeleteNotFound(String twink) {
        return config.telegram.menuTwinkDeleteNotFound.replace("{twink}", twink);
    }

    public String getMenuBackButton() {
        return config.telegram.menuBackButton;
    }

    public String getMenuManageButton() {
        return config.telegram.menuManageButton;
    }

    public String getMenuManageTitle() {
        return Colors.convertColorCodes(config.telegram.menuManageTitle);
    }

    public String getMenuPasswordButton() {
        return config.telegram.menuPasswordButton;
    }

    public String getMenuTwinkButton() {
        return config.telegram.menuTwinkButton;
    }

    public String getMenuPasswordYesButton() {
        return config.telegram.menuPasswordYesButton;
    }

    public String getMenuPasswordNoButton() {
        return config.telegram.menuPasswordNoButton;
    }

    public String getTelegramMenuPasswordConfirm(String playerName) {
        return format(config.telegram.menuPasswordConfirm, playerName);
    }

    public String getTelegramMenuPasswordSuccess(String playerName, String password) {
        return Colors.convertColorCodes(config.telegram.menuPasswordSuccess
                .replace("{player}", TelegramTextFormatter.escapeHtml(playerName))
                .replace("{password}", password));
    }

    public String getTelegramMenuPasswordError() {
        return Colors.convertColorCodes(config.telegram.menuPasswordError);
    }

    public String getTelegramMenuPasswordUnavailable() {
        return Colors.convertColorCodes(config.telegram.menuPasswordUnavailable);
    }

    public String getTelegramMenuPasswordNotRegistered(String playerName) {
        return format(config.telegram.menuPasswordNotRegistered, playerName);
    }

    public String getTelegramMenuPasswordCancelled() {
        return Colors.convertColorCodes(config.telegram.menuPasswordCancelled);
    }

    public String getMenuUnbindButton() {
        return config.telegram.menuUnbindButton;
    }

    public String getMenuUnbindYesButton() {
        return config.telegram.menuUnbindYesButton;
    }

    public String getMenuUnbindNoButton() {
        return config.telegram.menuUnbindNoButton;
    }

    public String getLoginConfirmText(String playerName, int timeoutSeconds) {
        return config.telegram.loginConfirmText
                .replace("{player}", TelegramTextFormatter.escapeHtml(playerName))
                .replace("{seconds}", String.valueOf(timeoutSeconds));
    }

    public String getLoginConfirmButton() {
        return config.telegram.loginConfirmButton;
    }

    public String getLoginDenyButton() {
        return config.telegram.loginDenyButton;
    }

    public String getLoginConfirmedAlert() {
        return config.telegram.loginConfirmedAlert;
    }

    public String getLoginConfirmed() {
        return config.telegram.loginConfirmed;
    }

    public String getLoginDeniedAlert() {
        return config.telegram.loginDeniedAlert;
    }

    public String getLoginDenied() {
        return config.telegram.loginDenied;
    }

    public String getUnknownAction() {
        return config.telegram.unknownAction;
    }

    public String getCommandMenuDescription() {
        return config.telegram.commandMenuDescription;
    }

    public String getCommandStartDescription() {
        return config.telegram.commandStartDescription;
    }

    private String format(String text, String playerName) {
        return Colors.convertColorCodes(text.replace("{player}", TelegramTextFormatter.escapeHtml(playerName)));
    }

    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class Config extends OkaeriConfig {

        public Telegram telegram = new Telegram();

        public static class Telegram extends OkaeriConfig {
            public String alreadyBound = "✅ Ваш Telegram аккаунт уже привязан к игроку: **{player}**";

            public String instructions = "**Привязка аккаунта Телеграм к Серверу**\n\n" +
                    "1. На сервере выполните команду ```py /binding code```\n" +
                    "2. Скопируйте полученный код (6 символов)\n" +
                    "3. Отправьте этот код боту в личные сообщения\n\n" +
                    "После этого твой аккаунт будет привязан!";

            public String instructionsVelocity = "**Привязка аккаунта Телеграм к Серверу**\n\n" +
                    "1. Зайдите на сервер, код привязки (6 символов) будет выдан сразу при входе\n" +
                    "2. Скопируйте полученный код\n" +
                    "3. Отправьте этот код боту в личные сообщения\n\n" +
                    "После этого ваш аккаунт будет привязан!";

            public String codeUsed = "❌ Этот код уже использован. Аккаунт игрока **{player}** уже привязан.";

            public String bindingSuccess = "✅ Аккаунт успешно привязан к игроку: **{player}**\nТеперь вы можете играть на сервере!";

            public String bindingError = "❌ Ошибка при привязке аккаунта. Попробуйте позже.";

            public String invalidCode = "❌ Неверный или просроченный код. Получите новый командой /binding code в игре.";

            public String tooManyAttempts = "⏳ Слишком много неверных кодов. Попробуйте снова через {minutes} мин.";

            public String menuTitle = "**📋 Меню**\n\nВыберите действие:";

            public String menuNotBound = "❌ Ваш Telegram аккаунт не привязан к игроку. Отправьте /start, чтобы узнать, как его привязать.";

            public String menuUnbindConfirm = "⚠️ Вы действительно хотите отвязать Telegram от игрока **{player}**?";

            public String menuUnbindSuccess = "✅ Telegram успешно отвязан от игрока **{player}**.";

            public String menuUnbindError = "❌ Не удалось отвязать аккаунт. Попробуйте позже.";

            public String menuUnbindCancelled = "Отвязка отменена.";

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
            public String menuTwinkPrompt = "➕ **Добавление твинка**\n\nОтправьте ник твинка следующим сообщением.\nСвободно мест: **{free}** из **{max}**.\n\n{list}";

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


            public String menuManageButton = "⚙️ Управление аккаунтом";

            public String menuManageTitle = "**⚙️ Управление аккаунтом**\n\nВыберите действие:";

            public String menuUnbindButton = "🔓 Отвязать аккаунт";

            public String menuPasswordButton = "🔑 Сменить пароль";

            public String menuTwinkButton = "👥 Твинки";

            public String menuPasswordYesButton = "✅ Да, сменить";

            public String menuPasswordNoButton = "❌ Отмена";

            public String menuPasswordConfirm = "⚠️ Сменить пароль игрока **{player}**? Старый пароль перестанет работать.";

            public String menuPasswordSuccess = "✅ Новый пароль игрока **{player}**:\n```{password}```\nСмените его в игре командой /changepassword.";

            public String menuPasswordError = "❌ Не удалось сменить пароль. Попробуйте позже.";

            public String menuPasswordUnavailable = "❌ Смена пароля сейчас недоступна.";

            public String menuPasswordNotRegistered = "❌ Игрок **{player}** не зарегистрирован в системе авторизации.";

            public String menuPasswordCancelled = "Смена пароля отменена.";

            public String menuUnbindYesButton = "✅ Да, отвязать";

            public String menuUnbindNoButton = "❌ Отмена";

            public String loginConfirmText = "**🔐 Подтверждение входа на сервер**\n\n" +
                    "Игрок **{player}** заходит на сервер.\n" +
                    "Если это вы - подтвердите вход в течение **{seconds} секунд**, иначе вы будете кикнуты.";

            public String loginConfirmButton = "✅ Подтвердить вход";

            public String loginDenyButton = "❌ Это не я";

            public String loginConfirmedAlert = "✅ Вход подтверждён";

            public String loginConfirmed = "✅ Вход подтверждён.";

            public String loginDeniedAlert = "❌ Вход отклонён";

            public String loginDenied = "❌ Вход отклонён, игрок кикнут с сервера.";

            public String unknownAction = "❓ Неизвестное действие";

            public String commandMenuDescription = "Открыть меню";

            public String commandStartDescription = "Инструкция по привязке";
        }
    }
}
