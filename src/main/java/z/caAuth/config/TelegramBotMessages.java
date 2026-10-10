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

    public String getTelegramInstructions() {
        return Colors.convertColorCodes(config.telegram.instructions);
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

            public String menuUnbindButton = "🔓 Отвязать аккаунт";

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
