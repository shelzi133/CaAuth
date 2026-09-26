package z.caAuth;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import z.caAuth.auth.account.AccountManager;
import z.caAuth.auth.account.AccountStorage;
import z.caAuth.bots.discord.Role.ActiveRoleCommandService;
import z.caAuth.bots.discord.Role.ActiveRoleSyncScheduler;
import z.caAuth.core.commands.ActivityPlayerCommand;
import z.caAuth.core.config.Okaeri;
import z.caAuth.bots.discord.DiscordBot;
import z.caAuth.bots.discord.DiscordBotMessages;
import z.caAuth.bots.discord.Role.LuckPermsRoleListener;
import z.caAuth.bots.discord.role.config.RolesConfig;
import z.caAuth.bots.telegram.TelegramBot;
import z.caAuth.bots.telegram.TelegramBotMessages;
import z.caAuth.auth.commands.BindingCommand;
import z.caAuth.auth.commands.BindingTabCompleter;
import z.caAuth.core.commands.CaAuthCommand;
import z.caAuth.auth.commands.UnbindCommand;
import z.caAuth.auth.config.AuthConfig;
import z.caAuth.auth.config.AuthMessagesConfig;
import z.caAuth.auth.login.LoginConfirmationFreezeListener;
import z.caAuth.auth.login.LoginConfirmationListener;
import z.caAuth.auth.login.LoginConfirmationManager;
import z.caAuth.auth.integration.BotIntegration;
import z.caAuth.bots.LoadBots;
import z.caAuth.core.config.MainConfig;
import z.caAuth.hook.PlanHook;
import z.caAuth.hook.TimeCounter;

public final class CaAuth extends JavaPlugin {

    private static volatile CaAuth instance;

    private AuthConfig authConfig;
    private AuthMessagesConfig authMessagesConfig;
    private MainConfig mainConfig;
    private TelegramBotMessages telegramBotMessages;
    private DiscordBotMessages discordBotMessages;
    private RolesConfig rolesConfig;

    private ActiveRoleSyncScheduler scheduler;
    private AccountStorage accountStorage;
    private AccountManager accountManager;
    private BotIntegration botIntegration;

    private LoadBots loadBots;
    private DiscordBot discordBot;
    private ActiveRoleCommandService activeRoleCommandService;
    private ActivityPlayerCommand activityPlayerCommand;

    private PlanHook planHook;
    private TimeCounter timeCounter;

    private LoginConfirmationManager loginConfirmationManager;

    @Override
    public void onEnable() {
        instance = this;

        java.util.logging.Logger.getLogger(
                "org.telegram.telegrambots.updatesreceivers.DefaultBotSession"
        ).setLevel(java.util.logging.Level.OFF);

        mainConfig = new Okaeri().loadMainConfig(this);
        authConfig = new Okaeri().loadConfig(this);
        authMessagesConfig = new Okaeri().loadMessages(this);
        telegramBotMessages = new TelegramBotMessages(this);
        discordBotMessages = new DiscordBotMessages(this);
        rolesConfig = new Okaeri().loadRolesConfig(this);
        planHook = new PlanHook(rolesConfig);

        accountStorage = new AccountStorage(getDataFolder());
        accountManager = new AccountManager(accountStorage);
        botIntegration = new BotIntegration(accountStorage, accountManager, this, authMessagesConfig);
        loginConfirmationManager = new LoginConfirmationManager(this, accountManager, authConfig, authMessagesConfig);


        loadBots = new LoadBots(this, authConfig, loginConfirmationManager, telegramBotMessages, discordBotMessages, rolesConfig, botIntegration, mainConfig);

        if (authConfig.bots.loginConfirmation.enabled) {
            getLogger().info("Подтверждение входа через ботов активировано (интервал: "
                    + authConfig.bots.loginConfirmation.intervalHours + " ч., таймаут: "
                    + authConfig.bots.loginConfirmation.timeoutSeconds + " сек.)");
        }
        getServer().getPluginManager().registerEvents(new LoginConfirmationListener(loginConfirmationManager), this);

        BindingTabCompleter bindingTabCompleter = new BindingTabCompleter();
        BindingCommand bindingCommand = new BindingCommand(accountManager, authMessagesConfig, mainConfig);
        getCommand("binding").setExecutor(bindingCommand);
        getCommand("binding").setTabCompleter(bindingTabCompleter);

        getCommand("unbind").setExecutor(new UnbindCommand(accountManager, authMessagesConfig));

        CaAuthCommand caAuthCommand = new CaAuthCommand(this, authConfig, authMessagesConfig, discordBotMessages,
                telegramBotMessages, rolesConfig, accountManager, mainConfig);
        getCommand("caauth").setExecutor(caAuthCommand);
        getCommand("caauth").setTabCompleter(caAuthCommand);

        LegacyPaperCommandManager<CommandSender> commandManager = LegacyPaperCommandManager.createNative(
                this,
                ExecutionCoordinator.simpleCoordinator()
        );

        getServer().getScheduler().runTask(this, () -> {
            timeCounter = planHook.hookIntoPlan().orElse(null);
            if (timeCounter == null) {
                getLogger().warning("Plan API не найден или не готов — роли по времени игры работать не будут.");
                return;
            }
            scheduler = new ActiveRoleSyncScheduler(accountStorage, discordBot, rolesConfig, timeCounter) ;
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, scheduler::checkAll, 20L, rolesConfig.getPeriod());
        });

        loadBots.checkBotConfiguration();
        loadBots.startBots();
        this.discordBot = loadBots.getDiscordBot();

        activeRoleCommandService = new ActiveRoleCommandService(accountStorage,discordBot);
        activityPlayerCommand = new ActivityPlayerCommand(activeRoleCommandService,authMessagesConfig);

        AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(
        commandManager,
                CommandSender.class
        );
        annotationParser.parse(activityPlayerCommand);

        new LuckPermsRoleListener(this, loadBots.getDiscordBot(), rolesConfig).register();

        getServer().getPluginManager().registerEvents(
                new LoginConfirmationFreezeListener(loginConfirmationManager), this
        );


        getLogger().info("CaAuth включен!");
    }

    @Override
    public void onDisable() {
        loadBots.stopBots();

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

    public AuthMessagesConfig getMessagesConfig() {
        return authMessagesConfig;
    }

    public LoginConfirmationManager getLoginConfirmationManager() {
        return loginConfirmationManager;
    }

    public DiscordBot getDiscordBot() {
        return loadBots.getDiscordBot();
    }

    public TelegramBot getTelegramBot() {
        return loadBots.getTelegramBot();
    }
}