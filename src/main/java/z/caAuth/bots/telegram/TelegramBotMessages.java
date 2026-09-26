package z.caAuth.bots.telegram;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.auth.util.ColorManager;

import java.io.File;

public class TelegramBotMessages {

    private final Config config;

    public TelegramBotMessages(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "bots/messages.yml");

        this.config = ConfigManager.create(Config.class, it -> {
            it.withConfigurer(new YamlBukkitConfigurer());
            it.withBindFile(configFile);
            it.withRemoveOrphans(false);
            it.saveDefaults();
            it.load(true);
        });
    }

    public void reload() {
        try {
            config.load(true);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось перезагрузить конфиг сообщений Telegram-бота", e);
        }
    }

    public void save() {
        config.save();
    }

    public String getTelegramAlreadyBound(String playerName) {
        return ColorManager.convertColorCodes(config.telegram.alreadyBound.replace("{player}", playerName));
    }

    public String getTelegramInstructions() {
        return ColorManager.convertColorCodes(config.telegram.instructions);
    }

    public String getTelegramUnknownCommand() {
        return ColorManager.convertColorCodes(config.telegram.unknownCommand);
    }

    public String getTelegramCodeUsed(String playerName) {
        return ColorManager.convertColorCodes(config.telegram.codeUsed.replace("{player}", playerName));
    }

    public String getTelegramBindingSuccess(String playerName) {
        return ColorManager.convertColorCodes(config.telegram.bindingSuccess.replace("{player}", playerName));
    }

    public String getTelegramBindingError() {
        return ColorManager.convertColorCodes(config.telegram.bindingError);
    }

    public String getTelegramInvalidCode() {
        return ColorManager.convertColorCodes(config.telegram.invalidCode);
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

            public String unknownCommand = "❓ Неизвестная команда. Отправьте код верификации (6 цифр) или /start для инструкций.";

            public String codeUsed = "❌ Этот код уже использован. Аккаунт игрока **{player}** уже привязан.";

            public String bindingSuccess = "✅ Аккаунт успешно привязан к игроку: **{player}**\nТеперь вы можете играть на сервере!";

            public String bindingError = "❌ Ошибка при привязке аккаунта. Попробуйте позже.";

            public String invalidCode = "❌ Неверный код верификации. Убедитесь, что вы ввели правильный код из игры.";
        }
    }
}