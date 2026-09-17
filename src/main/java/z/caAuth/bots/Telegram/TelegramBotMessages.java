package z.caAuth.bots.Telegram;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.util.ColorManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class TelegramBotMessages {
    private final JavaPlugin plugin;
    private final File configFile;
    private FileConfiguration config;
    
    public TelegramBotMessages(JavaPlugin plugin) {
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


    // тг
    public String getTelegramAlreadyBound(String playerName) {
        return ColorManager.convertColorCodes(config.getString("telegram.already-bound", 
                "✅ Ваш Telegram аккаунт уже привязан к игроку: **{player}**")
                .replace("{player}", playerName));
    }
    
    public String getTelegramInstructions() {
        return ColorManager.convertColorCodes(config.getString("telegram.instructions", 
                "**Привязка аккаунта Телеграм к Серверу**\n\n" +
                "1. На сервере выполните команду ```py /binding code```\n" +
                "2. Скопируйте полученный код (6 символов)\n" +
                "3. Отправьте этот код боту в личные сообщения\n\n" +
                "После этого твой аккаунт будет привязан!"));
    }
    
    public String getTelegramUnknownCommand() {
        return ColorManager.convertColorCodes(config.getString("telegram.unknown-command", 
                "❓ Неизвестная команда. Отправьте код верификации (6 цифр) или /start для инструкций."));
    }
    
    public String getTelegramCodeUsed(String playerName) {
        return ColorManager.convertColorCodes(config.getString("telegram.code-used", 
                "❌ Этот код уже использован. Аккаунт игрока **{player}** уже привязан.")
                .replace("{player}", playerName));
    }
    
    public String getTelegramBindingSuccess(String playerName) {
        return ColorManager.convertColorCodes(config.getString("telegram.binding-success", 
                "✅ Аккаунт успешно привязан к игроку: **{player}**\nТеперь вы можете играть на сервере!")
                .replace("{player}", playerName));
    }
    
    public String getTelegramBindingError() {
        return ColorManager.convertColorCodes(config.getString("telegram.binding-error", 
                "❌ Ошибка при привязке аккаунта. Попробуйте позже."));
    }
    
    public String getTelegramInvalidCode() {
        return ColorManager.convertColorCodes(config.getString("telegram.invalid-code", 
                "❌ Неверный код верификации. Убедитесь, что вы ввели правильный код из игры."));
    }
}

