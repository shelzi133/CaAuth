package z.caAuth;

import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import z.caAuth.account.AccountManager;
import z.caAuth.account.AccountStorage;
import z.caAuth.bots.Discord.DiscordBot;
import z.caAuth.bots.Discord.DiscordBotMessages;
import z.caAuth.bots.Discord.Role.LuckPermsRoleListener;
import z.caAuth.bots.Discord.Role.RolesConfig;
import z.caAuth.bots.Telegram.TelegramBot;
import z.caAuth.bots.Telegram.TelegramBotMessages;
import z.caAuth.commands.BindingCommand;
import z.caAuth.commands.BindingTabCompleter;
import z.caAuth.commands.CaAuthCommand;
import z.caAuth.commands.UnbindCommand;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.FirstJoin;
import z.caAuth.config.MessagesConfig;
import z.caAuth.confirmation.LoginConfirmationFreezeListener;
import z.caAuth.confirmation.LoginConfirmationListener;
import z.caAuth.confirmation.LoginConfirmationManager;
import z.caAuth.integration.BotIntegration;

public final class CaAuth extends JavaPlugin {

    private static volatile CaAuth instance;

    private AuthConfig authConfig;
    private MessagesConfig messagesConfig;
    private TelegramBotMessages telegramBotMessages;
    private DiscordBotMessages discordBotMessages;
    private RolesConfig rolesConfig;

    private AccountStorage accountStorage;
    private AccountManager accountManager;
    private BotIntegration botIntegration;

    private LoginConfirmationManager loginConfirmationManager;

    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private BotSession telegramBotSession;

    private FirstJoin firstjoin;

    @Override
    public void onEnable() {
        instance = this;

        java.util.logging.Logger.getLogger(
                "org.telegram.telegrambots.updatesreceivers.DefaultBotSession"
        ).setLevel(java.util.logging.Level.OFF);

        authConfig = new AuthConfig(this);
        messagesConfig = new MessagesConfig(this);
        telegramBotMessages = new TelegramBotMessages(this);
        discordBotMessages = new DiscordBotMessages(this);
        rolesConfig = new RolesConfig(this);
        firstjoin = new FirstJoin(messagesConfig);

        accountStorage = new AccountStorage(getDataFolder());
        accountManager = new AccountManager(accountStorage);
        botIntegration = new BotIntegration(accountStorage, accountManager, this, messagesConfig);

        if (authConfig.isLoginConfirmationEnabled()) {
            getLogger().info("Подтверждение входа через ботов активировано (интервал: "
                    + authConfig.getLoginConfirmationIntervalHours() + " ч., таймаут: "
                    + authConfig.getLoginConfirmationTimeoutSeconds() + " сек.)");
        }
        loginConfirmationManager = new LoginConfirmationManager(this, accountManager, authConfig, messagesConfig);
        getServer().getPluginManager().registerEvents(new LoginConfirmationListener(loginConfirmationManager), this);

        BindingTabCompleter bindingTabCompleter = new BindingTabCompleter();
        BindingCommand bindingCommand = new BindingCommand(accountManager, messagesConfig);
        getCommand("binding").setExecutor(bindingCommand);
        getCommand("binding").setTabCompleter(bindingTabCompleter);

        getCommand("unbind").setExecutor(new UnbindCommand(accountManager, messagesConfig));

        CaAuthCommand caAuthCommand = new CaAuthCommand(this, authConfig, messagesConfig, discordBotMessages,
                telegramBotMessages, rolesConfig, accountManager);
        getCommand("caauth").setExecutor(caAuthCommand);
        getCommand("caauth").setTabCompleter(caAuthCommand);

        checkBotConfiguration();
        startBots();

        new LuckPermsRoleListener(this, discordBot, rolesConfig).register();

        getServer().getPluginManager().registerEvents(
                new LoginConfirmationFreezeListener(loginConfirmationManager), this
        );

        getServer().getPluginManager().registerEvents(firstjoin, this);

        getLogger().info("CaAuth включен!");
    }

    @Override
    public void onDisable() {
        stopBots();

        if (accountStorage != null) {
            accountStorage.close();
        }

        getLogger().info("CaAuth выключен!");
    }

    public static CaAuth getInstance() {
        CaAuth local = instance;
        if (local == null) {
            throw new IllegalStateException("CaAuth is not initialized yet");
        }
        return local;
    }

    public AccountManager getAccountManager() {
        return accountManager;
    }

    public AccountStorage getAccountStorage() {
        return accountStorage;
    }

    public BotIntegration getBotIntegration() {
        return botIntegration;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public LoginConfirmationManager getLoginConfirmationManager() {
        return loginConfirmationManager;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public TelegramBot getTelegramBot() {
        return telegramBot;
    }

    private void startBots() {
        if (authConfig.isDiscordEnabled() && !authConfig.getDiscordBotToken().isEmpty()) {
            try {
                discordBot = new DiscordBot(this, botIntegration, discordBotMessages, rolesConfig, loginConfirmationManager,
                        authConfig.getDiscordBotToken(),
                        authConfig.getDiscordGuildId());
                discordBot.start();
                loginConfirmationManager.setDiscordBot(discordBot);
            } catch (Exception e) {
                getLogger().severe("Не удалось запустить Discord бота: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (authConfig.isTelegramEnabled() && !authConfig.getTelegramBotToken().isEmpty()) {
            try {
                DefaultBotOptions options = new DefaultBotOptions();
                org.apache.http.client.config.RequestConfig requestConfig =
                        org.apache.http.client.config.RequestConfig.custom()
                                .setConnectTimeout(15_000)
                                .setConnectionRequestTimeout(15_000)
                                .setSocketTimeout(75_000)
                                .build();
                options.setRequestConfig(requestConfig);

                if (authConfig.isTelegramProxyEnabled()) {
                    options.setProxyHost(authConfig.getTelegramProxyHost());
                    options.setProxyPort(authConfig.getTelegramProxyPort());
                    options.setProxyType(DefaultBotOptions.ProxyType.valueOf(
                            authConfig.getTelegramProxyType().toUpperCase()));

                    String proxyUser = authConfig.getTelegramProxyUsername();
                    String proxyPass = authConfig.getTelegramProxyPassword();
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

                    getLogger().info("Telegram бот запускается через прокси "
                            + authConfig.getTelegramProxyHost() + ":" + authConfig.getTelegramProxyPort()
                            + " (" + authConfig.getTelegramProxyType() + ")");
                }
                telegramBot = new TelegramBot(this, botIntegration, telegramBotMessages, loginConfirmationManager,
                        authConfig.getTelegramBotToken(), options);
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                telegramBotSession = botsApi.registerBot(telegramBot);
                telegramBot.start();
                loginConfirmationManager.setTelegramBot(telegramBot);
            } catch (TelegramApiException e) {
                getLogger().severe("Не удалось запустить Telegram бота: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                getLogger().severe("Ошибка при инициализации Telegram бота: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void stopBots() {
        if (discordBot != null) {
            discordBot.stop();
        }
        if (telegramBot != null) {
            telegramBot.stop();
        }
        //остановка long-polling поток (ReaderThread)
        //чтобы старые потоки не отправляли api.telegram.org.
        if (telegramBotSession != null && telegramBotSession.isRunning()) {
            telegramBotSession.stop();
        }
    }

    private void checkBotConfiguration() {
        if (authConfig.isDiscordEnabled() && authConfig.getDiscordBotToken().isEmpty()) {
            getLogger().warning("Discord бот включен, но токен не настроен! Установите токен в config.yml");
        }
        if (authConfig.isTelegramEnabled() && authConfig.getTelegramBotToken().isEmpty()) {
            getLogger().warning("Telegram бот включен, но токен не настроен! Установите токен в config.yml");
        }
    }
}