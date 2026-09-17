package z.caAuth.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import z.caAuth.CaAuth;
import z.caAuth.account.AccountManager;
import z.caAuth.bots.Discord.DiscordBotMessages;
import z.caAuth.bots.Telegram.TelegramBotMessages;
import z.caAuth.bots.Discord.DiscordBot;
import z.caAuth.bots.Discord.Role.RolesConfig;
import z.caAuth.bots.Telegram.TelegramBot;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.MessagesConfig;
import z.caAuth.util.BindingType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class CaAuthCommand implements CommandExecutor, TabCompleter {

    private final CaAuth plugin;
    private final AuthConfig authConfig;
    private final MessagesConfig messagesConfig;
    private final DiscordBotMessages discordBotMessages;
    private final TelegramBotMessages telegramBotMessages;
    private final RolesConfig rolesConfig;
    private final AccountManager accountManager;

    public CaAuthCommand(CaAuth plugin, AuthConfig authConfig, MessagesConfig messagesConfig,
                         DiscordBotMessages discordBotMessages, TelegramBotMessages telegramBotMessages,
                         RolesConfig rolesConfig, AccountManager accountManager) {
        this.plugin = plugin;
        this.authConfig = authConfig;
        this.messagesConfig = messagesConfig;
        this.discordBotMessages = discordBotMessages;
        this.telegramBotMessages = telegramBotMessages;
        this.rolesConfig = rolesConfig;
        this.accountManager = accountManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                return true;
            case "info":
                handleInfo(sender, args);
                return true;
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(messagesConfig.getCaauthUsageHeader());
        sender.sendMessage(messagesConfig.getCaauthUsageReload());
        sender.sendMessage(messagesConfig.getCaauthUsageInfo());
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("caauth.reload")) {
            sender.sendMessage(messagesConfig.getNoPermission());
            return;
        }
        authConfig.reload();
        messagesConfig.reload();
        telegramBotMessages.reload();
        discordBotMessages.reload();
        rolesConfig.reload();
        sender.sendMessage(messagesConfig.getConfigReloaded());
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("caauth.info")) {
            sender.sendMessage(messagesConfig.getNoPermission());
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messagesConfig.getCaauthInfoUsage());
            return;
        }

        String requestedName = args[1];
        Player onlinePlayer = Bukkit.getPlayerExact(requestedName);

        @SuppressWarnings("deprecation")
        OfflinePlayer targetPlayer = onlinePlayer != null
                ? onlinePlayer
                : Bukkit.getOfflinePlayer(requestedName);

        if (onlinePlayer == null && !targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(messagesConfig.getPlayerNeverPlayed(requestedName));
            return;
        }

        UUID uuid = targetPlayer.getUniqueId();
        String displayName = targetPlayer.getName() != null ? targetPlayer.getName() : requestedName;

        String ip = messagesConfig.getCaauthInfoIpUnavailable();
        String client = messagesConfig.getCaauthInfoClientUnavailable();
        if (onlinePlayer != null) {
            InetSocketAddress address = onlinePlayer.getAddress();
            ip = address != null && address.getAddress() != null
                    ? address.getAddress().getHostAddress()
                    : messagesConfig.getCaauthInfoValueUnknown();

            String brand = null;
            try {
                brand = onlinePlayer.getClientBrandName();
            } catch (Throwable ignored) {
            }
            client = brand != null && !brand.isBlank() ? brand : messagesConfig.getCaauthInfoValueUnknown();
        }

        String playtime = messagesConfig.getCaauthInfoPlaytimeUnknown();
        try {
            int ticks = targetPlayer.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long totalMinutes = ticks / (20L * 60L);
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            playtime = hours + " ч. " + minutes + " мин.";
        } catch (Throwable ignored) {
        }

        String binding = resolveBindingDisplay(uuid);

        sender.sendMessage(messagesConfig.getCaauthInfoTitle(displayName));
        sender.sendMessage(messagesConfig.getCaauthInfoSeparator());
        sender.sendMessage(messagesConfig.getCaauthInfoUuid(uuid.toString()));
        sender.sendMessage(messagesConfig.getCaauthInfoNick(displayName));
        sender.sendMessage(messagesConfig.getCaauthInfoIp(ip));
        sender.sendMessage(messagesConfig.getCaauthInfoClient(client));
        sender.sendMessage(messagesConfig.getCaauthInfoPlaytime(playtime));
        sender.sendMessage(messagesConfig.getCaauthInfoBinding(binding));
        sender.sendMessage(messagesConfig.getCaauthInfoSeparator());
    }

    private String resolveBindingDisplay(UUID uuid) {
        if (!accountManager.isAccountBound(uuid)) {
            return messagesConfig.getCaauthInfoNotBound();
        }

        String rawType = accountManager.getBindingType(uuid);
        String bindingId = accountManager.getBindingId(uuid);
        BindingType type = BindingType.fromString(rawType);

        String resolvedUser = null;
        String typeLabel;

        if (type == BindingType.TELEGRAM) {
            typeLabel = messagesConfig.getCaauthBindingLabelTelegram();
            TelegramBot telegramBot = plugin.getTelegramBot();
            if (telegramBot != null) {
                resolvedUser = telegramBot.resolveChatUserDisplay(bindingId);
            }
        } else {
            typeLabel = messagesConfig.getCaauthBindingLabelDiscord();
            DiscordBot discordBot = plugin.getDiscordBot();
            if (discordBot != null) {
                resolvedUser = discordBot.resolveUserDisplayById(bindingId);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(typeLabel);
        sb.append(" §7| §f").append(resolvedUser != null ? resolvedUser : messagesConfig.getCaauthInfoBindingUnknownUser());
        sb.append(" §7| ID: §f").append(bindingId);
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if (sender.hasPermission("caauth.reload") && "reload".startsWith(partial)) {
                completions.add("reload");
            }
            if (sender.hasPermission("caauth.info") && "info".startsWith(partial)) {
                completions.add("info");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")
                && sender.hasPermission("caauth.info")) {
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}
