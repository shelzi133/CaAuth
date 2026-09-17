package z.caAuth.bots.Discord;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.util.ColorManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class DiscordBotMessages {

    private final JavaPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public DiscordBotMessages(JavaPlugin plugin) {
        this.plugin = plugin;
        File botsFolder = new File(plugin.getDataFolder(), "bots");
        this.configFile = new File(botsFolder, "messages.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();

            try (InputStream defaultStream = plugin.getResource("bots/messages.yml")) {
                if (defaultStream != null) {
                    Files.copy(defaultStream, configFile.toPath());
                } else {
                    configFile.createNewFile();
                    createDefaultConfig();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать конфиг сообщений ботов: " + e.getMessage());
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }
    private void createDefaultConfig() {
    }
    public void reload() {
        loadConfig();
    }
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить конфиг сообщений ботов: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public String getDiscordAlreadyBound(String playerName) {
        return ColorManager.convertColorCodes(config.getString("discord.already-bound", "✅ Ваш Discord аккаунт уже привязан к игроку: **{player}**")
                .replace("{player}", playerName));
    }

    public String getDiscordInstructions() {
        return ColorManager.convertColorCodes(config.getString("discord.instructions",
                "**Привязка аккаунта дискорда к майну**\n\n" +
                        "1. В игре выполните команду ```/binding code```\n" +
                        "2. Скопируйте полученный код (6 символов)\n" +
                        "3. Отправьте этот код боту в личку\n\n" +
                        "После этого ваш аккаунт будет привязан!"));
    }

    public String getDiscordUnknownCommand() {
        return ColorManager.convertColorCodes(config.getString("discord.unknown-command",
                "❓ Неизвестная команда. Отправьте код верификации (6 цифр) или /start для инструкций."));
    }

    public String getDiscordCodeUsed(String playerName) {
        return ColorManager.convertColorCodes(config.getString("discord.code-used",
                        "❌ Этот код уже использован. Аккаунт игрока **{player}** уже привязан.")
                .replace("{player}", playerName));
    }

    public String getDiscordBindingSuccess(String playerName) {
        return ColorManager.convertColorCodes(config.getString("discord.binding-success",
                        "✅ Аккаунт успешно привязан к игроку: **{player}**\nТеперь вы можете играть на сервере!")
                .replace("{player}", playerName));
    }

    public String getDiscordBindingError() {
        return ColorManager.convertColorCodes(config.getString("discord.binding-error",
                "❌ Ошибка при привязке аккаунта. Попробуйте позже."));
    }

    public String getDiscordInvalidCode() {
        return ColorManager.convertColorCodes(config.getString("discord.invalid-code",
                "❌ Неверный код верификации. Убедитесь, что вы ввели правильный код из игры."));
    }
}
