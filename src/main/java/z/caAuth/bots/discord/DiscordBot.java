package z.caAuth.bots.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import okhttp3.OkHttpClient;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import z.caAuth.account.BindingType;
import z.caAuth.binding.BindingService;
import z.caAuth.bots.BotProxy;
import z.caAuth.bots.ChatBot;
import z.caAuth.bots.discord.role.DiscordRoleService;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.DiscordBotMessages;
import z.caAuth.config.RolesConfig;
import z.caAuth.integration.placeholderapi.OnlineCounter;
import z.caAuth.login.LoginConfirmationManager;

import java.awt.Color;
import java.time.Duration;
import java.util.Collections;

public class DiscordBot extends ListenerAdapter implements ChatBot {

    private static final String LOGIN_CONFIRM_PREFIX = "login_confirm_";
    private static final String LOGIN_DENY_PREFIX = "login_deny_";
    private static final long PRESENCE_UPDATE_PERIOD_TICKS = 200L;

    private final JavaPlugin plugin;
    private final LoginConfirmationManager confirmationManager;
    private final DiscordMessageHandler messageHandler;
    private final DiscordRoleService roleService;
    private final DiscordBotMessages messages;
    private final OnlineCounter onlineCounter;
    private final String token;
    private final String guildId;
    private final AuthConfig authConfig;
    private final AuthConfig.Proxy proxy;

    private JDA jda;
    private boolean isRunning = false;
    private BukkitTask presenceUpdateTask;
    private int lastPresenceCount = -1;

    public DiscordBot(JavaPlugin plugin, BindingService bindingService, DiscordBotMessages messagesConfig,
                      RolesConfig rolesConfig, LoginConfirmationManager confirmationManager,
                      String token, String guildId, AuthConfig authConfig) {
        this.plugin = plugin;
        this.confirmationManager = confirmationManager;
        this.messages = messagesConfig;
        this.onlineCounter = new OnlineCounter(plugin);
        this.token = token;
        this.guildId = guildId;
        this.authConfig = authConfig;
        this.proxy = BotProxy.appliesTo(authConfig.proxy, BindingType.DISCORD) ? authConfig.proxy : null;
        this.roleService = new DiscordRoleService(plugin, bindingService, rolesConfig, this);
        this.messageHandler = new DiscordMessageHandler(bindingService, messagesConfig, roleService);
    }

    @Override
    public BindingType type() {
        return BindingType.DISCORD;
    }

    @Override
    public void start() {
        if (token == null || token.isEmpty()) {
            plugin.getLogger().warning("Discord токен не настроен!");
            return;
        }

        try {
            JDABuilder builder = JDABuilder.createDefault(token)
                    .addEventListeners(this);
            if (proxy != null) {
                BotProxy.installAuthenticator(proxy);
                builder.setHttpClientBuilder(BotProxy.applyTo(new OkHttpClient.Builder(), proxy));
                builder.setWebsocketFactory(BotProxy.webSocketFactory(proxy));
                plugin.getLogger().info("Discord бот запускается через прокси "
                        + proxy.host + ":" + proxy.port + " (" + proxy.type + ")");
            }
            jda = builder.build();
            jda.awaitReady();

            isRunning = true;
            startPresenceUpdater();
            plugin.getLogger().info("Discord бот успешно запущен!");
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("4004") || errorMsg.contains("AUTHENTICATION_FAILED"))) {
                plugin.getLogger().severe("Ошибка аутентификации Discord бота: токен неверный или истек. Проверьте bots.discord.token в config.yml");
            } else {
                plugin.getLogger().severe("Ошибка при запуске Discord бота: " + errorMsg);
                e.printStackTrace();
            }
            isRunning = false;
        }
    }

    @Override
    public void stop() {
        stopPresenceUpdater();
        if (jda != null) {
            isRunning = false;
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(Duration.ofSeconds(5))) {
                    jda.shutdownNow();
                    jda.awaitShutdown(Duration.ofSeconds(5));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                jda.shutdownNow();
            }
            plugin.getLogger().info("Discord бот остановлен.");
        }
    }

    private void startPresenceUpdater() {
        stopPresenceUpdater();
        lastPresenceCount = -1;
        presenceUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> updatePresence(false), 5L, PRESENCE_UPDATE_PERIOD_TICKS);
    }

    public int updatePresence(boolean force) {
        if (jda == null || !isRunning || jda.getStatus() != JDA.Status.CONNECTED) return -1;
        try {
            int online = onlineCounter.count() * authConfig.bots.discord.fakeOnline;
            if (force || online != lastPresenceCount) {
                lastPresenceCount = online;
                jda.getPresence().setActivity(Activity.watching(messages.getPresence(online)));
            }
            return online;
        } catch (Exception e) {
            plugin.getLogger().warning("Discord presence update: " + e.getMessage());
            return -1;
        }
    }

    private void stopPresenceUpdater() {
        if (presenceUpdateTask != null) {
            presenceUpdateTask.cancel();
            presenceUpdateTask = null;
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning && jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    public JDA getJda() {
        return jda;
    }

    public String getGuildId() {
        return guildId;
    }

    @Override
    public String resolveDisplay(String discordUserId) {
        if (!isRunning() || discordUserId == null || discordUserId.isBlank()) return null;
        try {
            long snowflake = Long.parseLong(discordUserId.trim());
            User u = jda.retrieveUserById(snowflake).complete();
            String username = u.getName();
            String global = u.getGlobalName();
            return (global != null && !global.isBlank()) ? global + " (@" + username + ")" : "@" + username;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean sendLoginConfirmation(String discordId, String playerName, String token, int timeoutSeconds) {
        if (jda == null || !isRunning() || discordId == null || discordId.isBlank()) return false;
        try {
            long snowflake = Long.parseLong(discordId.trim());
            jda.retrieveUserById(snowflake).queue(user -> {
                user.openPrivateChannel().queue(channel -> {
                    Button confirmBtn = Button.success(LOGIN_CONFIRM_PREFIX + token, messages.getLoginConfirmButton());
                    Button denyBtn = Button.danger(LOGIN_DENY_PREFIX + token, messages.getLoginDenyButton());

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(messages.getLoginConfirmTitle());
                    embed.setDescription(messages.getLoginConfirmDescription(playerName, timeoutSeconds));
                    embed.setColor(new Color(0xFFA500));

                    channel.sendMessageEmbeds(embed.build())
                            .addActionRow(confirmBtn, denyBtn)
                            .queue(success -> {}, failure -> plugin.getLogger().warning(
                                    "Не удалось отправить запрос подтверждения входа в Discord: " + failure.getMessage()));
                }, failure -> plugin.getLogger().warning(
                        "Не удалось открыть личные сообщения Discord для подтверждения входа: " + failure.getMessage()));
            }, failure -> plugin.getLogger().warning(
                    "Не удалось найти Discord пользователя для подтверждения входа: " + failure.getMessage()));
            return true;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Некорректный Discord ID для подтверждения входа: " + discordId);
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при отправке запроса подтверждения входа в Discord: " + e.getMessage());
            return false;
        }
    }

    public DiscordRoleService getRoleService() {
        return roleService;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (componentId.startsWith(LOGIN_CONFIRM_PREFIX)) {
            String loginToken = componentId.substring(LOGIN_CONFIRM_PREFIX.length());
            confirmationManager.confirm(loginToken);
            event.editMessageEmbeds(
                    new EmbedBuilder().setDescription(messages.getLoginConfirmed()).setColor(new Color(0x00FF00)).build()
            ).setComponents(Collections.emptyList()).queue();
        } else if (componentId.startsWith(LOGIN_DENY_PREFIX)) {
            String loginToken = componentId.substring(LOGIN_DENY_PREFIX.length());
            confirmationManager.deny(loginToken);
            event.editMessageEmbeds(
                    new EmbedBuilder().setDescription(messages.getLoginDenied()).setColor(new Color(0xFF0000)).build()
            ).setComponents(Collections.emptyList()).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) {
            messageHandler.handlePrivateMessage(event);
        }
    }
}