package z.caAuth.bots.discord;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.auth.util.ColorManager;

import java.io.File;

public class DiscordBotMessages {

    private final Config config;

    public DiscordBotMessages(JavaPlugin plugin) {
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
            throw new RuntimeException("Не удалось перезагрузить конфиг сообщений Discord-бота", e);
        }
    }

    public void save() {
        config.save();
    }

    public String getDiscordAlreadyBound(String playerName) {
        return ColorManager.convertColorCodes(config.discord.alreadyBound.replace("{player}", playerName));
    }

    public String getDiscordInstructions() {
        return ColorManager.convertColorCodes(config.discord.instructions);
    }

    public String getDiscordUnknownCommand() {
        return ColorManager.convertColorCodes(config.discord.unknownCommand);
    }

    public String getDiscordCodeUsed(String playerName) {
        return ColorManager.convertColorCodes(config.discord.codeUsed.replace("{player}", playerName));
    }

    public String getDiscordBindingSuccess(String playerName) {
        return ColorManager.convertColorCodes(config.discord.bindingSuccess.replace("{player}", playerName));
    }

    public String getDiscordBindingError() {
        return ColorManager.convertColorCodes(config.discord.bindingError);
    }

    public String getDiscordMenuMessage(String playerName) {
        return ColorManager.convertColorCodes(config.discord.bindingSuccess.replace("{player}", playerName));
    }

    public String getDiscordInvalidCode() {
        return ColorManager.convertColorCodes(config.discord.invalidCode);
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

            public String unknownCommand = "❓ Неизвестная команда. Отправьте код верификации (6 цифр) или /start для инструкций.";

            public String codeUsed = "❌ Этот код уже использован. Аккаунт игрока **{player}** уже привязан.";

            public String bindingSuccess = "✅ Аккаунт успешно привязан к игроку: **{player}**\nТеперь вы можете играть на сервере!";

            public String bindingError = "❌ Ошибка при привязке аккаунта. Попробуйте позже.";

            public String invalidCode = "❌ Неверный код верификации. Убедитесь, что вы ввели правильный код из игры.";
        }
    }
}