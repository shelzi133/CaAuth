package z.caAuth.bots;

import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import z.caAuth.auth.config.AuthConfig;
import z.caAuth.auth.integration.BotIntegration;
import z.caAuth.auth.login.LoginConfirmationManager;
import z.caAuth.bots.discord.DiscordBot;
import z.caAuth.bots.discord.DiscordBotMessages;
import z.caAuth.bots.discord.role.config.RolesConfig;
import z.caAuth.bots.telegram.TelegramBot;
import z.caAuth.bots.telegram.TelegramBotMessages;
import z.caAuth.core.config.MainConfig;

public class LoadBots {

    private final JavaPlugin plugin;

    private final AuthConfig authConfig;
    private final MainConfig mainConfig;
    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private BotSession telegramBotSession;
    private final LoginConfirmationManager loginConfirmationManager;
    private final TelegramBotMessages telegramBotMessages;
    private final DiscordBotMessages discordBotMessages;
    private final RolesConfig rolesConfig;
    private final BotIntegration botIntegration;

    public LoadBots(JavaPlugin plugin, AuthConfig authConfig, LoginConfirmationManager loginConfirmationManager,
                    TelegramBotMessages telegramBotMessages, DiscordBotMessages discordBotMessages,
                    RolesConfig rolesConfig, BotIntegration botIntegration, MainConfig mainConfig) {
        this.plugin = plugin;
        this.authConfig = authConfig;
        this.loginConfirmationManager = loginConfirmationManager;
        this.telegramBotMessages = telegramBotMessages;
        this.discordBotMessages = discordBotMessages;
        this.rolesConfig = rolesConfig;
        this.botIntegration = botIntegration;
        this.mainConfig = mainConfig;
    }

    public void startBots() {
        if (!mainConfig.module.auth.enabled){
            plugin.getLogger().severe("Модуль auth выключен");
            return;
        }
        if (authConfig.bots.discord.enabled && !authConfig.bots.discord.token.isEmpty()) {
            try {
                discordBot = new DiscordBot(plugin, botIntegration, discordBotMessages, rolesConfig, loginConfirmationManager,
                        authConfig.bots.discord.token,
                        authConfig.bots.discord.guildId);
                discordBot.start();
                loginConfirmationManager.setDiscordBot(discordBot);
            } catch (Exception e) {
                plugin.getLogger().severe("Не удалось запустить Discord бота: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (authConfig.bots.telegram.enabled && !authConfig.bots.telegram.token.isEmpty()) {
            try {
                DefaultBotOptions options = new DefaultBotOptions();
                org.apache.http.client.config.RequestConfig requestConfig =
                        org.apache.http.client.config.RequestConfig.custom()
                                .setConnectTimeout(15_000)
                                .setConnectionRequestTimeout(15_000)
                                .setSocketTimeout(75_000)
                                .build();
                options.setRequestConfig(requestConfig);

                if (authConfig.bots.telegram.proxy.enabled) {
                    options.setProxyHost(authConfig.bots.telegram.proxy.host);
                    options.setProxyPort(authConfig.bots.telegram.proxy.port);
                    options.setProxyType(DefaultBotOptions.ProxyType.valueOf(
                            authConfig.bots.telegram.proxy.type.toUpperCase()));

                    String proxyUser = authConfig.bots.telegram.proxy.username;
                    String proxyPass = authConfig.bots.telegram.proxy.password;
                    if (proxyUser != null && !proxyUser.isEmpty()) {
                        final String finalUser = proxyUser;
                        final char[] finalPass = (proxyPass == null ? "" : proxyPass).toCharArray();
                        java.net.Authenticator.setDefault(new java.net.Authenticator() {
                            @Override
                            protected java.net.PasswordAuthentication getPasswordAuthentication() {
                                return new java.net.PasswordAuthentication(finalUser, finalPass);
                            }
                        });
                    }

                    plugin.getLogger().info("Telegram бот запускается через прокси "
                            + authConfig.bots.telegram.proxy.host + ":" + authConfig.bots.telegram.proxy.port
                            + " (" + authConfig.bots.telegram.proxy.type + ")");
                }
                telegramBot = new TelegramBot(plugin, botIntegration, telegramBotMessages, loginConfirmationManager,
                        authConfig.bots.telegram.token, options);
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                telegramBotSession = botsApi.registerBot(telegramBot);
                telegramBot.start();
                loginConfirmationManager.setTelegramBot(telegramBot);
            } catch (TelegramApiException e) {
                plugin.getLogger().severe("Не удалось запустить Telegram бота: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка при инициализации Telegram бота: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void stopBots() {
        if (discordBot != null) {
            discordBot.stop();
        }
        if (telegramBot != null) {
            telegramBot.stop();
        }
        if (telegramBotSession != null && telegramBotSession.isRunning()) {
            telegramBotSession.stop();
        }
    }

    public void checkBotConfiguration() {
        if (authConfig.bots.discord.enabled && authConfig.bots.discord.token.isEmpty()) {
            plugin.getLogger().warning("Discord бот включен, но токен не настроен! Установите токен в config.yml");
        }
        if (authConfig.bots.telegram.enabled && authConfig.bots.telegram.token.isEmpty()) {
            plugin.getLogger().warning("Telegram бот включен, но токен не настроен! Установите токен в config.yml");
        }
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public TelegramBot getTelegramBot() {
        return telegramBot;
    }
}