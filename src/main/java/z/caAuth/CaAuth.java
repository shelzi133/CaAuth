package z.caAuth;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import z.caAuth.account.AccountManager;
import z.caAuth.account.AccountRepository;
import z.caAuth.account.RegisteredPlayerNames;
import z.caAuth.binding.BindingLookup;
import z.caAuth.binding.BindingService;
import z.caAuth.binding.CodeAttemptLimiter;
import z.caAuth.bots.BotManager;
import z.caAuth.bots.discord.role.ActiveRoleService;
import z.caAuth.bots.discord.role.ActiveRoleSyncScheduler;
import z.caAuth.commands.ActivityPlayerCommand;
import z.caAuth.commands.BindingCommand;
import z.caAuth.commands.CaAuthCommand;
import z.caAuth.commands.CommandExceptionHandler;
import z.caAuth.commands.UnbindCommand;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.AuthMessagesConfig;
import z.caAuth.config.ConfigLoader;
import z.caAuth.config.DiscordBotMessages;
import z.caAuth.config.MainConfig;
import z.caAuth.config.RolesConfig;
import z.caAuth.config.TelegramBotMessages;
import z.caAuth.integration.luckperms.LuckPermsRoleListener;
import z.caAuth.integration.plan.PlanHook;
import z.caAuth.integration.plan.PlaytimeProvider;
import z.caAuth.integration.velocity.VelocityBridge;
import z.caAuth.login.LoginConfirmationFreezeListener;
import z.caAuth.login.LoginConfirmationListener;
import z.caAuth.login.LoginConfirmationManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class CaAuth extends JavaPlugin {

    private MainConfig mainConfig;
    private AuthConfig authConfig;
    private AuthMessagesConfig authMessagesConfig;
    private TelegramBotMessages telegramBotMessages;
    private DiscordBotMessages discordBotMessages;
    private RolesConfig rolesConfig;

    private AccountRepository accountRepository;
    private AccountManager accountManager;
    private BindingService bindingService;
    private LoginConfirmationManager loginConfirmationManager;

    private BotManager botManager;
    private VelocityBridge velocityBridge;

    @Override
    public void onEnable() {
        Logger.getLogger("org.telegram.telegrambots.updatesreceivers.DefaultBotSession").setLevel(Level.OFF);

        try {
            loadConfigs();
        } catch (IllegalStateException e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e.getCause());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initServices();
        startBots();
        registerCommands();
        registerListeners();
        hookPlanNextTick();

        getLogger().info("CaAuth включен!");
    }

    @Override
    public void onDisable() {
        if (velocityBridge != null) {
            velocityBridge.stop();
        }
        if (botManager != null) {
            botManager.stopBots();
        }
        if (accountRepository != null) {
            accountRepository.close();
        }
        getLogger().info("CaAuth выключен!");
    }

    private void loadConfigs() {
        mainConfig = ConfigLoader.load(this, MainConfig.class, "config.yml");
        authConfig = ConfigLoader.load(this, AuthConfig.class, "auth/auth-config.yml");
        authMessagesConfig = ConfigLoader.load(this, AuthMessagesConfig.class, "auth/auth-messages.yml");
        rolesConfig = ConfigLoader.load(this, RolesConfig.class, "bots/roles.yml");
        telegramBotMessages = new TelegramBotMessages(this);
        discordBotMessages = new DiscordBotMessages(this);
    }

    private void initServices() {
        accountRepository = new AccountRepository(getDataFolder(), getLogger());
        accountManager = new AccountManager(accountRepository, authConfig);
        bindingService = new BindingService(accountManager, this, authMessagesConfig, new CodeAttemptLimiter(authConfig));
        loginConfirmationManager = new LoginConfirmationManager(this, accountManager, authConfig, authMessagesConfig);
        botManager = new BotManager(this, authConfig, loginConfirmationManager, telegramBotMessages, discordBotMessages,
                rolesConfig, bindingService, mainConfig);

        if (authConfig.bots.loginConfirmation.enabled) {
            getLogger().info("Подтверждение входа через ботов активировано (интервал: "
                    + authConfig.bots.loginConfirmation.intervalHours + " ч., таймаут: "
                    + authConfig.bots.loginConfirmation.timeoutSeconds + " сек.)");
        }
    }

    private void startBots() {
        botManager.checkBotConfiguration();
        botManager.startBots();

        velocityBridge = new VelocityBridge(this, mainConfig, authConfig, accountManager, loginConfirmationManager);
        velocityBridge.start();
    }

    private void registerCommands() {
        RegisteredPlayerNames registeredPlayerNames = new RegisteredPlayerNames(this, accountManager);
        registeredPlayerNames.refreshAsync();

        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this)
                .exceptionHandler(new CommandExceptionHandler(authMessagesConfig))
                .suggestionProviders(providers ->
                        providers.addProvider(OfflinePlayer.class, context -> registeredPlayerNames.get()))
                .build();

        lamp.register(
                new BindingCommand(accountManager, authMessagesConfig, mainConfig),
                new UnbindCommand(accountManager, authMessagesConfig),
                new ActivityPlayerCommand(
                        new ActiveRoleService(accountManager, botManager::getDiscordBot), authMessagesConfig),
                new CaAuthCommand(this, authConfig, authMessagesConfig, discordBotMessages,
                        telegramBotMessages, rolesConfig, accountManager, mainConfig, botManager,
                        new BindingLookup(accountManager, botManager)));
    }

    private void registerListeners() {
        if (!mainConfig.isVelocityMode()) {
            getServer().getPluginManager().registerEvents(new LoginConfirmationListener(loginConfirmationManager), this);
        }
        getServer().getPluginManager().registerEvents(new LoginConfirmationFreezeListener(loginConfirmationManager, authMessagesConfig), this);

        new LuckPermsRoleListener(this, botManager::getDiscordBot, rolesConfig).register();
    }

    private void hookPlanNextTick() {
        getServer().getScheduler().runTask(this, () -> {
            PlaytimeProvider playtimeProvider = getServer().getPluginManager().isPluginEnabled("Plan")
                    ? new PlanHook(rolesConfig).hookIntoPlan().orElse(null)
                    : null;
            if (playtimeProvider == null) {
                getLogger().warning("Plan API не найден или не готов — роли по времени игры работать не будут.");
                return;
            }
            getServer().getPluginManager().registerEvents(
                    new ActiveRoleSyncScheduler(this, accountManager, botManager::getDiscordBot, rolesConfig, playtimeProvider),
                    this);
        });
    }
}
