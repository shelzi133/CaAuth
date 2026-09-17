package z.caAuth.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class AuthConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public AuthConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public void save() {
        plugin.saveConfig();
    }

    public boolean isDiscordEnabled() {
        return config.getBoolean("bots.discord.enabled", false);
    }

    public void setDiscordEnabled(boolean enabled) {
        config.set("bots.discord.enabled", enabled);
        save();
    }

    public String getDiscordBotToken() {
        return config.getString("bots.discord.token", "");
    }

    public void setDiscordBotToken(String token) {
        config.set("bots.discord.token", token);
        save();
    }

    public String getDiscordGuildId() {
        return config.getString("bots.discord.guild-id", "");
    }

    public void setDiscordGuildId(String guildId) {
        config.set("bots.discord.guild-id", guildId);
        save();
    }

    public boolean isTelegramEnabled() {
        return config.getBoolean("bots.telegram.enabled", false);
    }

    public void setTelegramEnabled(boolean enabled) {
        config.set("bots.telegram.enabled", enabled);
        save();
    }

    public String getTelegramBotToken() {
        return config.getString("bots.telegram.token", "");
    }

    public void setTelegramBotToken(String token) {
        config.set("bots.telegram.token", token);
        save();
    }

    public boolean isTelegramProxyEnabled() {
        return config.getBoolean("bots.telegram.proxy.enabled", false);
    }

    public String getTelegramProxyType() {
        return config.getString("bots.telegram.proxy.type", "SOCKS5");
    }

    public String getTelegramProxyHost() {
        return config.getString("bots.telegram.proxy.host", "127.0.0.1");
    }

    public int getTelegramProxyPort() {
        return config.getInt("bots.telegram.proxy.port", 1080);
    }

    public String getTelegramProxyUsername() {
        return config.getString("bots.telegram.proxy.username", "");
    }

    public String getTelegramProxyPassword() {
        return config.getString("bots.telegram.proxy.password", "");
    }

    public boolean areBotsConfigured() {
        boolean discordConfigured = !isDiscordEnabled() || !getDiscordBotToken().isEmpty();
        boolean telegramConfigured = !isTelegramEnabled() || !getTelegramBotToken().isEmpty();
        return discordConfigured && telegramConfigured;
    }

    public boolean isLoginConfirmationEnabled() {
        return config.getBoolean("login-confirmation.enabled", true);
    }

    public void setLoginConfirmationEnabled(boolean enabled) {
        config.set("login-confirmation.enabled", enabled);
        save();
    }

    public int getLoginConfirmationIntervalHours() {
        return config.getInt("login-confirmation.interval-hours", 6);
    }

    public void setLoginConfirmationIntervalHours(int hours) {
        config.set("login-confirmation.interval-hours", hours);
        save();
    }

    public int getLoginConfirmationTimeoutSeconds() {
        return config.getInt("login-confirmation.timeout-seconds", 60);
    }

    public void setLoginConfirmationTimeoutSeconds(int seconds) {
        config.set("login-confirmation.timeout-seconds", seconds);
        save();
    }
}
