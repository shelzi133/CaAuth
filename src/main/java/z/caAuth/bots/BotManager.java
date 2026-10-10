package z.caAuth.bots;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import z.caAuth.account.BindingType;
import z.caAuth.binding.BindingService;
import z.caAuth.bots.discord.DiscordBot;
import z.caAuth.bots.telegram.TelegramBot;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.DiscordBotMessages;
import z.caAuth.config.MainConfig;
import z.caAuth.config.RolesConfig;
import z.caAuth.config.TelegramBotMessages;
import z.caAuth.login.LoginConfirmationManager;

import java.util.concurrent.locks.ReentrantLock;

public class BotManager {

    public enum Result {
        OK,
        AUTH_DISABLED,
        BOT_DISABLED,
        TOKEN_MISSING,
        ALREADY_RUNNING,
        NOT_RUNNING,
        BUSY,
        FAILED
    }

    private final JavaPlugin plugin;
    private final AuthConfig authConfig;
    private final MainConfig mainConfig;
    private final LoginConfirmationManager loginConfirmationManager;
    private final TelegramBotMessages telegramBotMessages;
    private final DiscordBotMessages discordBotMessages;
    private final RolesConfig rolesConfig;
    private final BindingService bindingService;

    private final ReentrantLock discordLock = new ReentrantLock();
    private final ReentrantLock telegramLock = new ReentrantLock();

    private volatile DiscordBot discordBot;
    private volatile TelegramBot telegramBot;
    private volatile BotSession telegramBotSession;

    public BotManager(JavaPlugin plugin, AuthConfig authConfig, LoginConfirmationManager loginConfirmationManager,
                      TelegramBotMessages telegramBotMessages, DiscordBotMessages discordBotMessages,
                      RolesConfig rolesConfig, BindingService bindingService, MainConfig mainConfig) {
        this.plugin = plugin;
        this.authConfig = authConfig;
        this.loginConfirmationManager = loginConfirmationManager;
        this.telegramBotMessages = telegramBotMessages;
        this.discordBotMessages = discordBotMessages;
        this.rolesConfig = rolesConfig;
        this.bindingService = bindingService;
        this.mainConfig = mainConfig;
    }

    public void startBots() {
        if (!mainConfig.module.auth.enabled) {
            plugin.getLogger().severe("Модуль auth выключен");
            return;
        }
        startWithRetry(BindingType.DISCORD);
        startWithRetry(BindingType.TELEGRAM);
    }

    private void startWithRetry(BindingType platform) {
        if (startBot(platform) == Result.FAILED) {
            scheduleRetry(platform, 1);
        }
    }

    private void scheduleRetry(BindingType platform, int attempt) {
        AuthConfig.Proxy proxy = authConfig.proxy;
        if (!BotProxy.appliesTo(proxy, platform) || attempt > proxy.retryAttempts) {
            return;
        }
        long delaySeconds = Math.max(1, proxy.retryDelaySeconds);
        plugin.getLogger().warning("Повторный запуск " + platform.getId() + " бота через "
                + delaySeconds + " сек. (попытка " + attempt + " из " + proxy.retryAttempts + ")");
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            Result result = startBot(platform);
            if (result == Result.FAILED || result == Result.BUSY) {
                scheduleRetry(platform, attempt + 1);
            }
        }, delaySeconds * 20L);
    }

    public void stopBots() {
        discordLock.lock();
        try {
            stopDiscord();
        } finally {
            discordLock.unlock();
        }
        telegramLock.lock();
        try {
            stopTelegram();
        } finally {
            telegramLock.unlock();
        }
    }

    public Result startBot(BindingType platform) {
        ReentrantLock lock = lockOf(platform);
        if (!lock.tryLock()) {
            return Result.BUSY;
        }
        try {
            Result check = checkConfig(platform);
            if (check != Result.OK) {
                return check;
            }
            if (isRunning(platform)) {
                return Result.ALREADY_RUNNING;
            }
            return launch(platform) ? Result.OK : Result.FAILED;
        } finally {
            lock.unlock();
        }
    }

    public Result stopBot(BindingType platform) {
        ReentrantLock lock = lockOf(platform);
        if (!lock.tryLock()) {
            return Result.BUSY;
        }
        try {
            if (!isRunning(platform)) {
                return Result.NOT_RUNNING;
            }
            halt(platform);
            return isRunning(platform) ? Result.FAILED : Result.OK;
        } finally {
            lock.unlock();
        }
    }

    public Result restartBot(BindingType platform) {
        ReentrantLock lock = lockOf(platform);
        if (!lock.tryLock()) {
            return Result.BUSY;
        }
        try {
            Result check = checkConfig(platform);
            if (check != Result.OK) {
                return check;
            }
            halt(platform);
            return launch(platform) ? Result.OK : Result.FAILED;
        } finally {
            lock.unlock();
        }
    }

    public boolean isRunning(BindingType platform) {
        return platform == BindingType.DISCORD ? isDiscordRunning() : isTelegramRunning();
    }

    public boolean isDiscordRunning() {
        DiscordBot bot = discordBot;
        return bot != null && bot.isRunning();
    }

    public boolean isTelegramRunning() {
        BotSession session = telegramBotSession;
        return telegramBot != null && session != null && session.isRunning();
    }

    private ReentrantLock lockOf(BindingType platform) {
        return platform == BindingType.DISCORD ? discordLock : telegramLock;
    }

    private Result checkConfig(BindingType platform) {
        if (!mainConfig.module.auth.enabled) {
            return Result.AUTH_DISABLED;
        }
        boolean enabled;
        String token;
        if (platform == BindingType.DISCORD) {
            enabled = authConfig.bots.discord.enabled;
            token = authConfig.bots.discord.token;
        } else {
            enabled = authConfig.bots.telegram.enabled;
            token = authConfig.bots.telegram.token;
        }
        if (!enabled) {
            return Result.BOT_DISABLED;
        }
        if (token == null || token.isBlank()) {
            return Result.TOKEN_MISSING;
        }
        return Result.OK;
    }

    private boolean launch(BindingType platform) {
        boolean launched = platform == BindingType.DISCORD ? launchDiscord() : launchTelegram();
        return launched && isRunning(platform);
    }

    private void halt(BindingType platform) {
        if (platform == BindingType.DISCORD) {
            stopDiscord();
        } else {
            stopTelegram();
        }
    }

    private boolean launchDiscord() {
        try {
            DiscordBot bot = new DiscordBot(plugin, bindingService, discordBotMessages, rolesConfig,
                    loginConfirmationManager,
                    authConfig.bots.discord.token,
                    authConfig.bots.discord.guildId,
                    authConfig);
            discordBot = bot;
            bot.start();
            loginConfirmationManager.registerBot(bot);
            if (!bot.isRunning()) {
                bot.stop();
                return false;
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось запустить Discord бота: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean launchTelegram() {
        try {
            DefaultBotOptions options = new DefaultBotOptions();
            org.apache.http.client.config.RequestConfig requestConfig =
                    org.apache.http.client.config.RequestConfig.custom()
                            .setConnectTimeout(15_000)
                            .setConnectionRequestTimeout(15_000)
                            .setSocketTimeout(75_000)
                            .build();
            options.setRequestConfig(requestConfig);

            AuthConfig.Proxy proxy = authConfig.proxy;
            if (BotProxy.appliesTo(proxy, BindingType.TELEGRAM)) {
                options.setProxyHost(proxy.host);
                options.setProxyPort(proxy.port);
                options.setProxyType(DefaultBotOptions.ProxyType.valueOf(proxy.type.toUpperCase()));
                BotProxy.installAuthenticator(proxy);
                plugin.getLogger().info("Telegram бот запускается через прокси "
                        + proxy.host + ":" + proxy.port + " (" + proxy.type + ")");
            }
            TelegramBot bot = new TelegramBot(plugin, bindingService, telegramBotMessages, loginConfirmationManager,
                    authConfig.bots.telegram.token, options);
            telegramBot = bot;
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotSession = botsApi.registerBot(bot);
            bot.start();
            loginConfirmationManager.registerBot(bot);
            return true;
        } catch (TelegramApiException e) {
            plugin.getLogger().severe("Не удалось запустить Telegram бота: " + e.getMessage());
            e.printStackTrace();
            stopTelegram();
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при инициализации Telegram бота: " + e.getMessage());
            e.printStackTrace();
            stopTelegram();
            return false;
        }
    }

    private void stopDiscord() {
        DiscordBot bot = discordBot;
        if (bot == null) {
            return;
        }
        try {
            bot.stop();
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при остановке Discord бота: " + e.getMessage());
        }
    }

    private void stopTelegram() {
        TelegramBot bot = telegramBot;
        if (bot != null) {
            try {
                bot.stop();
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при остановке Telegram бота: " + e.getMessage());
            }
        }
        BotSession session = telegramBotSession;
        if (session != null) {
            try {
                if (session.isRunning()) {
                    session.stop();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при остановке Telegram сессии: " + e.getMessage());
            }
            telegramBotSession = null;
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

    public ChatBot getBot(BindingType type) {
        return type == BindingType.DISCORD ? discordBot : telegramBot;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }
}