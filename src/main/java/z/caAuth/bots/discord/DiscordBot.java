package z.caAuth.bots.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import z.caAuth.auth.integration.BotIntegration;
import z.caAuth.auth.login.LoginConfirmationManager;
import z.caAuth.auth.util.ColorManager;
import z.caAuth.bots.discord.role.config.RolesConfig;

import java.awt.Color;
import java.util.Collections;

public class DiscordBot extends ListenerAdapter implements DiscordGuildAccessor {

    private static final String LOGIN_CONFIRM_PREFIX = "login_confirm_";
    private static final String LOGIN_DENY_PREFIX = "login_deny_";
    private static final long PRESENCE_UPDATE_PERIOD_TICKS = 40L;

    private final JavaPlugin plugin;
    private final LoginConfirmationManager confirmationManager;
    private final DiscordMessageHandler messageHandler;
    private final DiscordRoleService roleService;
    private final String token;
    private final String guildId;

    private JDA jda;
    private boolean isRunning = false;
    private BukkitTask presenceUpdateTask;

    public DiscordBot(JavaPlugin plugin, BotIntegration botIntegration, DiscordBotMessages messagesConfig,
                       RolesConfig rolesConfig, LoginConfirmationManager confirmationManager,
                       String token, String guildId) {
        this.plugin = plugin;
        this.confirmationManager = confirmationManager;
        this.token = token;
        this.guildId = guildId;
        this.roleService = new DiscordRoleService(plugin, botIntegration, rolesConfig, this);
        this.messageHandler = new DiscordMessageHandler(plugin, botIntegration, messagesConfig, roleService);
    }

    public void start() {
        if (token == null || token.isEmpty()) {
            plugin.getLogger().warning("Discord токен не настроен!");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(this)
                    .build();
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

    public void stop() {
        stopPresenceUpdater();
        if (jda != null) {
            jda.shutdown();
            isRunning = false;
            plugin.getLogger().info("Discord бот остановлен.");
        }
    }

    private void startPresenceUpdater() {
        stopPresenceUpdater();
        presenceUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (jda == null || !isRunning || jda.getStatus() != JDA.Status.CONNECTED) return;
            try {
                int online = Bukkit.getOnlinePlayers().size();
                jda.getPresence().setActivity(Activity.watching("Онлайн: " + online));
            } catch (Exception e) {
                plugin.getLogger().fine("Discord presence update: " + e.getMessage());
            }
        }, 5L, PRESENCE_UPDATE_PERIOD_TICKS);
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

    @Override
    public JDA getJda() {
        return jda;
    }

    @Override
    public String getGuildId() {
        return guildId;
    }

    public String resolveUserDisplayById(String discordUserId) {
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

    public boolean sendLoginConfirmation(String discordId, String playerName, String token, int timeoutSeconds) {
        if (jda == null || !isRunning() || discordId == null || discordId.isBlank()) return false;
        try {
            long snowflake = Long.parseLong(discordId.trim());
            jda.retrieveUserById(snowflake).queue(user -> {
                user.openPrivateChannel().queue(channel -> {
                    Button confirmBtn = Button.success(LOGIN_CONFIRM_PREFIX + token, "✅ Подтвердить вход");
                    Button denyBtn = Button.danger(LOGIN_DENY_PREFIX + token, "❌ Это не я");

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("🔐 Подтверждение входа на сервер");
                    embed.setDescription("Игрок **" + playerName + "** заходит на сервер.\n" +
                            "Если это вы - подтвердите вход в течение **" + timeoutSeconds + " секунд**, иначе вы будете кикнуты.");
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

    public void sendMessageToUser(String discordId, String message) {
        if (jda == null || !isRunning()) return;
        try {
            User user = jda.retrieveUserById(discordId).complete();
            if (user == null) return;

            user.openPrivateChannel().queue(channel -> {
                if (ColorManager.containsHexColors(message)) {
                    String hexColor = ColorManager.extractHexColor(message);
                    int colorInt = hexColor != null ? ColorManager.hexToInt(hexColor) : -1;
                    String cleanText = ColorManager.processDiscordColors(message);

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setDescription(cleanText);
                    if (colorInt != -1) embed.setColor(new Color(colorInt));
                    channel.sendMessageEmbeds(embed.build()).queue();
                } else {
                    channel.sendMessage(message).queue();
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при отправке сообщения в Discord: " + e.getMessage());
        }
    }

    public boolean isUserInGuild(String userId) {
        if (guildId == null || guildId.isEmpty() || jda == null) return true;
        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return false;
            return guild.getMemberById(userId) != null;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при проверке участника сервера: " + e.getMessage());
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
                    new EmbedBuilder().setDescription("✅ Вход подтверждён.").setColor(new Color(0x00FF00)).build()
            ).setComponents(Collections.emptyList()).queue();
        } else if (componentId.startsWith(LOGIN_DENY_PREFIX)) {
            String loginToken = componentId.substring(LOGIN_DENY_PREFIX.length());
            confirmationManager.deny(loginToken);
            event.editMessageEmbeds(
                    new EmbedBuilder().setDescription("❌ Вход отклонён, игрок кикнут с сервера.").setColor(new Color(0xFF0000)).build()
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
