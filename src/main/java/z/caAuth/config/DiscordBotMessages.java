package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.util.Colors;

public class DiscordBotMessages {

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

    public String getDiscordInstructions() {
        return Colors.convertColorCodes(config.discord.instructions);
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
        }
    }
}
