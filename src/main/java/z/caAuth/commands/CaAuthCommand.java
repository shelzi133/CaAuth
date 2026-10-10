package z.caAuth.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import z.caAuth.account.AccountManager;
import z.caAuth.account.BindingType;
import z.caAuth.binding.BindingLookup;
import z.caAuth.bots.BotManager;
import z.caAuth.bots.discord.DiscordBot;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.AuthMessagesConfig;
import z.caAuth.config.DiscordBotMessages;
import z.caAuth.config.MainConfig;
import z.caAuth.config.RolesConfig;
import z.caAuth.config.TelegramBotMessages;

import java.net.InetSocketAddress;
import java.util.UUID;

@Command("caauth")
public class CaAuthCommand {

    private enum Action { START, STOP, RESTART }

    private final JavaPlugin plugin;
    private final AuthConfig authConfig;
    private final AuthMessagesConfig authMessagesConfig;
    private final DiscordBotMessages discordBotMessages;
    private final MainConfig mainConfig;
    private final TelegramBotMessages telegramBotMessages;
    private final RolesConfig rolesConfig;
    private final AccountManager accountManager;
    private final BotManager botManager;
    private final BindingLookup lookup;

    public CaAuthCommand(JavaPlugin plugin, AuthConfig authConfig, AuthMessagesConfig authMessagesConfig,
                         DiscordBotMessages discordBotMessages, TelegramBotMessages telegramBotMessages,
                         RolesConfig rolesConfig, AccountManager accountManager, MainConfig mainConfig,
                         BotManager botManager, BindingLookup lookup) {
        this.plugin = plugin;
        this.authConfig = authConfig;
        this.authMessagesConfig = authMessagesConfig;
        this.discordBotMessages = discordBotMessages;
        this.telegramBotMessages = telegramBotMessages;
        this.rolesConfig = rolesConfig;
        this.accountManager = accountManager;
        this.mainConfig = mainConfig;
        this.botManager = botManager;
        this.lookup = lookup;
    }

    @CommandPlaceholder
    public void usage(BukkitCommandActor actor) {
        CommandSender sender = actor.sender();
        sender.sendMessage(authMessagesConfig.getCaauthUsageHeader());
        sender.sendMessage(authMessagesConfig.getCaauthUsageReload());
        sender.sendMessage(authMessagesConfig.getCaauthUsageInfo());
        sender.sendMessage(authMessagesConfig.getCaauthUsageDiscord());
        sender.sendMessage(authMessagesConfig.getCaauthUsageTelegram());
    }

    @Subcommand("reload")
    @CommandPermission("caauth.admin")
    public void reload(BukkitCommandActor actor) {
        CommandSender sender = actor.sender();

        boolean authReloaded = authConfig.reload();

        authMessagesConfig.reload();
        telegramBotMessages.reload();
        discordBotMessages.reload();
        rolesConfig.reload();
        mainConfig.reload();

        if (authReloaded) {
            sender.sendMessage(authMessagesConfig.getConfigReloaded());
        } else {
            sender.sendMessage(authMessagesConfig.getErrorPrefix()
                    + "Не удалось перезагрузить config.yml, проверьте структуру файла");
        }
    }

    @Subcommand("ds start")
    @CommandPermission("caauth.admin")
    public void dsStart(BukkitCommandActor actor) {
        runBotAction(actor.sender(), BindingType.DISCORD, Action.START);
    }

    @Subcommand("ds stop")
    @CommandPermission("caauth.admin")
    public void dsStop(BukkitCommandActor actor) {
        runBotAction(actor.sender(), BindingType.DISCORD, Action.STOP);
    }

    @Subcommand("ds restart")
    @CommandPermission("caauth.admin")
    public void dsRestart(BukkitCommandActor actor) {
        runBotAction(actor.sender(), BindingType.DISCORD, Action.RESTART);
    }

    @Subcommand("ds online-reload")
    @CommandPermission("caauth.admin")
    public void dsOnlineReload(BukkitCommandActor actor) {
        CommandSender sender = actor.sender();
        String bot = botName(BindingType.DISCORD);
        DiscordBot discordBot = botManager.getDiscordBot();
        if (discordBot == null || !discordBot.isRunning()) {
            sender.sendMessage(authMessagesConfig.getBotsNotRunning(bot));
            return;
        }
        int online = discordBot.updatePresence(true);
        if (online < 0) {
            sender.sendMessage(authMessagesConfig.getBotsOnlineReloadFailed(bot));
            return;
        }
        sender.sendMessage(authMessagesConfig.getBotsOnlineReloaded(bot, online));
    }

    @Subcommand("ds info")
    @CommandPermission("caauth.admin")
    public void dsInfo(BukkitCommandActor actor, String query) {
        runBotInfo(actor.sender(), BindingType.DISCORD, "ds", query);
    }

    @Subcommand("tg start")
    @CommandPermission("caauth.admin")
    public void tgStart(BukkitCommandActor actor) {
        runBotAction(actor.sender(), BindingType.TELEGRAM, Action.START);
    }

    @Subcommand("tg stop")
    @CommandPermission("caauth.admin")
    public void tgStop(BukkitCommandActor actor) {
        runBotAction(actor.sender(), BindingType.TELEGRAM, Action.STOP);
    }

    @Subcommand("tg restart")
    @CommandPermission("caauth.admin")
    public void tgRestart(BukkitCommandActor actor) {
        runBotAction(actor.sender(), BindingType.TELEGRAM, Action.RESTART);
    }

    @Subcommand("tg info")
    @CommandPermission("caauth.admin")
    public void tgInfo(BukkitCommandActor actor, String query) {
        runBotInfo(actor.sender(), BindingType.TELEGRAM, "tg", query);
    }

    @Subcommand("info")
    @CommandPermission("caauth.admin")
    public void info(BukkitCommandActor actor, OfflinePlayer target) {
        showPlayerInfo(actor.sender(), target, resolveBindingDisplay(target.getUniqueId()));
    }

    private String botName(BindingType platform) {
        return platform == BindingType.DISCORD
                ? authMessagesConfig.getBotsDiscordName()
                : authMessagesConfig.getBotsTelegramName();
    }

    private void runBotAction(CommandSender sender, BindingType platform, Action action) {
        String bot = botName(platform);
        sender.sendMessage(switch (action) {
            case START -> authMessagesConfig.getBotsStarting(bot);
            case STOP -> authMessagesConfig.getBotsStopping(bot);
            case RESTART -> authMessagesConfig.getBotsRestarting(bot);
        });

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            BotManager.Result result;
            try {
                result = switch (action) {
                    case START -> botManager.startBot(platform);
                    case STOP -> botManager.stopBot(platform);
                    case RESTART -> botManager.restartBot(platform);
                };
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка действия с ботом: " + e);
                sender.sendMessage(authMessagesConfig.getBotsActionError(bot, e.getMessage()));
                return;
            }
            sender.sendMessage(describeResult(bot, action, result));
        });
    }

    private String describeResult(String bot, Action action, BotManager.Result result) {
        return switch (result) {
            case OK -> switch (action) {
                case START -> authMessagesConfig.getBotsStarted(bot);
                case STOP -> authMessagesConfig.getBotsStopped(bot);
                case RESTART -> authMessagesConfig.getBotsRestarted(bot);
            };
            case AUTH_DISABLED -> authMessagesConfig.getErrorPrefix() + authMessagesConfig.getModuleAuthDisabled();
            case BOT_DISABLED -> authMessagesConfig.getBotsDisabledInConfig(bot);
            case TOKEN_MISSING -> authMessagesConfig.getBotsTokenMissing(bot);
            case ALREADY_RUNNING -> authMessagesConfig.getBotsAlreadyRunning(bot);
            case NOT_RUNNING -> authMessagesConfig.getBotsNotRunning(bot);
            case BUSY -> authMessagesConfig.getBotsBusy(bot);
            case FAILED -> authMessagesConfig.getBotsFailed(bot);
        };
    }

    private void runBotInfo(CommandSender sender, BindingType platform, String commandName, String rawQuery) {
        String bot = botName(platform);
        String query = rawQuery == null ? "" : rawQuery.trim();
        while (query.startsWith("@")) {
            query = query.substring(1);
        }
        if (query.isEmpty()) {
            sender.sendMessage(authMessagesConfig.getBotsInfoUsage(commandName));
            return;
        }

        String finalQuery = query;
        boolean numeric = finalQuery.chars().allMatch(Character::isDigit);
        sender.sendMessage(authMessagesConfig.getBotsInfoSearching(bot));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean botUp = botManager.isRunning(platform);

                UUID uuid = numeric ? lookup.findByBindingId(platform, finalQuery) : null;
                if (uuid == null && botUp) {
                    uuid = lookup.findByDisplayName(platform, finalQuery);
                }

                if (uuid == null) {
                    sender.sendMessage(botUp || numeric
                            ? authMessagesConfig.getBotsInfoNotFound(bot, finalQuery)
                            : authMessagesConfig.getBotsInfoBotRequired(bot));
                    return;
                }

                String binding = resolveBindingDisplay(uuid);
                OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
                plugin.getServer().getScheduler().runTask(plugin, () -> showPlayerInfo(sender, target, binding));
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка поиска аккаунта бота: " + e);
                sender.sendMessage(authMessagesConfig.getBotsActionError(bot, e.getMessage()));
            }
        });
    }

    private void showPlayerInfo(CommandSender sender, OfflinePlayer target, String binding) {
        Player onlinePlayer = target.getPlayer();

        String displayName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        if (onlinePlayer == null && !target.hasPlayedBefore()) {
            sender.sendMessage(authMessagesConfig.getPlayerNeverPlayed(displayName));
            return;
        }

        UUID uuid = target.getUniqueId();

        String ip = authMessagesConfig.getCaauthInfoIpUnavailable();
        String client = authMessagesConfig.getCaauthInfoClientUnavailable();
        if (onlinePlayer != null) {
            InetSocketAddress address = onlinePlayer.getAddress();
            ip = address != null && address.getAddress() != null
                    ? address.getAddress().getHostAddress()
                    : authMessagesConfig.getCaauthInfoValueUnknown();

            String brand = null;
            try {
                brand = onlinePlayer.getClientBrandName();
            } catch (Throwable ignored) {
            }
            client = brand != null && !brand.isBlank() ? brand : authMessagesConfig.getCaauthInfoValueUnknown();
        }

        String playtime = authMessagesConfig.getCaauthInfoPlaytimeUnknown();
        try {
            int ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long totalMinutes = ticks / (20L * 60L);
            playtime = (totalMinutes / 60) + " ч. " + (totalMinutes % 60) + " мин.";
        } catch (Throwable ignored) {
        }

        sender.sendMessage(authMessagesConfig.getCaauthInfoTitle(displayName));
        sender.sendMessage(authMessagesConfig.getCaauthInfoSeparator());
        sender.sendMessage(authMessagesConfig.getCaauthInfoUuid(uuid.toString()));
        sender.sendMessage(authMessagesConfig.getCaauthInfoNick(displayName));
        sender.sendMessage(authMessagesConfig.getCaauthInfoIp(ip));
        sender.sendMessage(authMessagesConfig.getCaauthInfoClient(client));
        sender.sendMessage(authMessagesConfig.getCaauthInfoPlaytime(playtime));
        sender.sendMessage(authMessagesConfig.getCaauthInfoBinding(binding));
        sender.sendMessage(authMessagesConfig.getCaauthInfoSeparator());
    }

    private String resolveBindingDisplay(UUID uuid) {
        if (!accountManager.isAccountBound(uuid)) {
            return authMessagesConfig.getCaauthInfoNotBound();
        }

        BindingType type = accountManager.getBindingType(uuid);
        String bindingId = accountManager.getBindingId(uuid);

        String typeLabel = type == BindingType.TELEGRAM
                ? authMessagesConfig.getCaauthBindingLabelTelegram()
                : authMessagesConfig.getCaauthBindingLabelDiscord();
        String resolvedUser = lookup.resolveDisplay(type, bindingId);

        return typeLabel
                + " §7| §f" + (resolvedUser != null ? resolvedUser : authMessagesConfig.getCaauthInfoBindingUnknownUser())
                + " §7| ID: §f" + bindingId;
    }
}