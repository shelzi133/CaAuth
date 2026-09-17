package z.caAuth.bots.Discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.bots.Discord.Role.RolesConfig;
import z.caAuth.confirmation.LoginConfirmationManager;
import z.caAuth.integration.BotIntegration;
import z.caAuth.util.ColorManager;

import java.awt.Color;
import java.util.UUID;
import java.util.regex.Pattern;


public class DiscordBot extends ListenerAdapter {
    private final JavaPlugin plugin;
    private final BotIntegration botIntegration;
    private final DiscordBotMessages messagesConfig;
    private final RolesConfig rolesConfig;
    private final LoginConfirmationManager confirmationManager;
    private final String token;
    private final String guildId;
    private JDA jda;
    private boolean isRunning = false;
    private BukkitTask presenceUpdateTask;

    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");
    private static final String LOGIN_CONFIRM_PREFIX = "login_confirm_";
    private static final String LOGIN_DENY_PREFIX = "login_deny_";
    private static final long PRESENCE_UPDATE_PERIOD_TICKS = 40L;

    public DiscordBot(JavaPlugin plugin, BotIntegration botIntegration, DiscordBotMessages messagesConfig,
                      RolesConfig rolesConfig, LoginConfirmationManager confirmationManager,
                      String token, String guildId) {
        this.plugin = plugin;
        this.botIntegration = botIntegration;
        this.messagesConfig = messagesConfig;
        this.rolesConfig = rolesConfig;
        this.confirmationManager = confirmationManager;
        this.token = token;
        this.guildId = guildId;
    }
    public void start() {
        if (token == null || token.isEmpty()) {
            plugin.getLogger().warning("Discord токен не настроен!");
            return;
        }

        try {
            JDABuilder builder = JDABuilder.createDefault(token);

            jda = builder
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
            if (jda == null || !isRunning) {
                return;
            }
            if (jda.getStatus() != net.dv8tion.jda.api.JDA.Status.CONNECTED) {
                return;
            }
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

    public boolean isRunning() {
        return isRunning && jda != null && jda.getStatus() == net.dv8tion.jda.api.JDA.Status.CONNECTED;
    }


    public String resolveUserDisplayById(String discordUserId) {
        if (!isRunning() || discordUserId == null || discordUserId.isBlank()) {
            return null;
        }
        try {
            long snowflake = Long.parseLong(discordUserId.trim());
            User u = jda.retrieveUserById(snowflake).complete();
            String username = u.getName();
            String global = u.getGlobalName();
            if (global != null && !global.isBlank()) {
                return global + " (@" + username + ")";
            }
            return "@" + username;
        } catch (Exception e) {
            return null;
        }
    }
    public boolean sendLoginConfirmation(String discordId, String playerName, String token, int timeoutSeconds) {
        if (jda == null || !isRunning() || discordId == null || discordId.isBlank()) {
            return false;
        }
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
                            .queue(
                                    success -> {},
                                    failure -> plugin.getLogger().warning(
                                            "Не удалось отправить запрос подтверждения входа в Discord: " + failure.getMessage())
                            );
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

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (componentId.startsWith(LOGIN_CONFIRM_PREFIX)) {
            String loginToken = componentId.substring(LOGIN_CONFIRM_PREFIX.length());
            confirmationManager.confirm(loginToken);
            event.editMessageEmbeds(
                    new EmbedBuilder().setDescription("✅ Вход подтверждён.").setColor(new Color(0x00FF00)).build()
            ).setComponents(java.util.Collections.emptyList()).queue();
        } else if (componentId.startsWith(LOGIN_DENY_PREFIX)) {
            String loginToken = componentId.substring(LOGIN_DENY_PREFIX.length());
            confirmationManager.deny(loginToken);
            event.editMessageEmbeds(
                    new EmbedBuilder().setDescription("❌ Вход отклонён, игрок кикнут с сервера.").setColor(new Color(0xFF0000)).build()
            ).setComponents(java.util.Collections.emptyList()).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (!event.isFromGuild()) {
            handlePrivateMessage(event);
        }
    }

    private void handlePrivateMessage(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw().trim();
        String discordId = event.getAuthor().getId();
        UUID existingPlayerUUID = botIntegration.getPlayerByDiscordId(discordId);
        if (existingPlayerUUID != null) {
            String playerName = botIntegration.getPlayerName(existingPlayerUUID);
            sendDiscordMessage(event, messagesConfig.getDiscordAlreadyBound(playerName));
            return;
        }
        if (CODE_PATTERN.matcher(message).matches()) {
            handleVerificationCode(event, message, discordId);
            return;
        }
        if (!message.equalsIgnoreCase("/start") && !message.equalsIgnoreCase("/help") && !message.equalsIgnoreCase("start")) {
            handlePlayerNameBinding(event, message, discordId);
            return;
        }
        if (message.equalsIgnoreCase("/start") || message.equalsIgnoreCase("/help") || message.equalsIgnoreCase("start")) {
            sendInstructions(event);
        }
    }

    private void handleVerificationCode(MessageReceivedEvent event, String code, String discordId) {
        boolean found = false;

        for (org.bukkit.OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            UUID playerUUID = offlinePlayer.getUniqueId();

            String pendingCode = botIntegration.getAccountStorage().getPendingCode(playerUUID);
            if (pendingCode != null && pendingCode.equals(code)) {
                if (botIntegration.isAccountBound(playerUUID)) {
                    sendDiscordMessage(event, messagesConfig.getDiscordCodeUsed(offlinePlayer.getName()));
                    return;
                }
                if (botIntegration.bindDiscordAccount(playerUUID, discordId)) {
                    String playerName = offlinePlayer.getName();
                    sendDiscordMessage(event, messagesConfig.getDiscordBindingSuccess(playerName));

                    assignRoleAndNicknameIfEnabled(discordId, playerUUID, playerName);

                    found = true;
                    break;
                } else {
                    sendDiscordMessage(event, messagesConfig.getDiscordBindingError());
                    return;
                }
            }
        }

        if (!found) {
            sendDiscordMessage(event, messagesConfig.getDiscordInvalidCode());
        }
    }

    private void handlePlayerNameBinding(MessageReceivedEvent event, String playerName, String discordId) {
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerName);

        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            sendInstructions(event);
            return;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();
        if (botIntegration.isAccountBound(playerUUID)) {
            sendDiscordMessage(event, messagesConfig.getDiscordCodeUsed(offlinePlayer.getName()));
            return;
        }
        String pendingCode = botIntegration.getAccountStorage().getPendingCode(playerUUID);
        if (pendingCode == null || pendingCode.isEmpty()) {
            sendDiscordMessage(event, "❌ У игрока **" + playerName + "** нет активного кода верификации. Попросите игрока выполнить команду `/binding code` в игре.");
            return;
        }
        if (botIntegration.bindDiscordAccount(playerUUID, discordId)) {
            sendDiscordMessage(event, messagesConfig.getDiscordBindingSuccess(offlinePlayer.getName()));
            assignRoleAndNicknameIfEnabled(discordId, playerUUID, offlinePlayer.getName());
        } else {
            sendDiscordMessage(event, messagesConfig.getDiscordBindingError());
        }
    }

    private void sendInstructions(MessageReceivedEvent event) {
        sendDiscordMessage(event, messagesConfig.getDiscordInstructions());
    }
    private void sendDiscordMessage(MessageReceivedEvent event, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (ColorManager.containsHexColors(text)) {
            String hexColor = ColorManager.extractHexColor(text);
            int colorInt = hexColor != null ? ColorManager.hexToInt(hexColor) : -1;
            String cleanText = ColorManager.processDiscordColors(text);
            EmbedBuilder embed = new EmbedBuilder();
            embed.setDescription(cleanText);

            if (colorInt != -1) {
                embed.setColor(new Color(colorInt));
            }

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        } else {
            event.getChannel().sendMessage(text).queue();
        }
    }

    public void sendMessageToUser(String discordId, String message) {
        if (jda == null || !isRunning()) {
            return;
        }

        try {
            User user = jda.retrieveUserById(discordId).complete();
            if (user != null) {
                user.openPrivateChannel().queue(channel -> {
                    if (ColorManager.containsHexColors(message)) {
                        String hexColor = ColorManager.extractHexColor(message);
                        int colorInt = hexColor != null ? ColorManager.hexToInt(hexColor) : -1;
                        String cleanText = ColorManager.processDiscordColors(message);

                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setDescription(cleanText);
                        if (colorInt != -1) {
                            embed.setColor(new Color(colorInt));
                        }
                        channel.sendMessageEmbeds(embed.build()).queue();
                    } else {
                        channel.sendMessage(message).queue();
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при отправке сообщения в Discord: " + e.getMessage());
        }
    }
    public boolean isUserInGuild(String userId) {
        if (guildId == null || guildId.isEmpty() || jda == null) {
            return true;
        }

        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) {
                return false;
            }

            return guild.getMemberById(userId) != null;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при проверке участника сервера: " + e.getMessage());
            return false;
        }
    }
    private void assignRoleAndNicknameIfEnabled(String discordId, UUID playerUUID, String playerName) {

        if (guildId == null || guildId.isEmpty()) return;
        if (jda == null || !isRunning()) return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            plugin.getLogger().warning("Guild не найден");
            return;
        }

        guild.retrieveMemberById(discordId).queue(member -> {

            if (rolesConfig != null && rolesConfig.isEnabled()) {
                String roleId = rolesConfig.getRoleId();
                if (roleId != null && !roleId.isEmpty()) {

                    Role role = guild.getRoleById(roleId);
                    if (role != null && !member.getRoles().contains(role)) {

                        guild.addRoleToMember(member, role).queue(
                                success -> plugin.getLogger().info(
                                        "Роль " + role.getName() + " выдана " + member.getUser().getAsTag()
                                ),
                                failure -> plugin.getLogger().warning(
                                        "Ошибка выдачи роли: " + failure.getMessage()
                                )
                        );
                    }
                }
            }

            if (rolesConfig != null && rolesConfig.isChangeNicknameEnabled()) {

                String currentNickname = member.getEffectiveName();

                if (!currentNickname.equals(playerName)) {

                    String nickname = playerName.length() > 32
                            ? playerName.substring(0, 32)
                            : playerName;

                    guild.modifyNickname(member, nickname).queue(
                            success -> plugin.getLogger().info(
                                    "Ник изменён на " + nickname
                            ),
                            failure -> plugin.getLogger().warning(
                                    "Ошибка смены ника: " + failure.getMessage()
                            )
                    );
                }
            }

        }, error -> {
            plugin.getLogger().warning(
                    "Discord пользователь " + discordId + " не найден или не на сервере: " + error.getMessage()
            );
        });
    }



    public void syncDonateRole(UUID playerUUID, String highestTier) {
        if (rolesConfig == null || !rolesConfig.isDonateRole()) return;
        if (jda == null || !isRunning()) return;
        if (guildId == null || guildId.isEmpty()) return;

        String discordId = botIntegration.getDiscordId(playerUUID);
        if (discordId == null || discordId.isBlank()) return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        guild.retrieveMemberById(discordId).queue(member -> {
            for (String group : rolesConfig.getDonateGroups()) {
                String roleId = rolesConfig.getRoleIdForGroup(group);
                if (roleId == null || roleId.isEmpty()) continue;

                Role role = guild.getRoleById(roleId);
                if (role == null) continue;

                boolean shouldHave = group.equalsIgnoreCase(highestTier);
                boolean hasRole = member.getRoles().contains(role);

                if (shouldHave && !hasRole) {
                    guild.addRoleToMember(member, role).queue(
                            success -> plugin.getLogger().info(
                                    "Донат-роль " + role.getName() + " выдана " + member.getUser().getAsTag()
                            ),
                            failure -> plugin.getLogger().warning(
                                    "Ошибка выдачи донат-роли: " + failure.getMessage()
                            )
                    );
                } else if (!shouldHave && hasRole) {
                    guild.removeRoleFromMember(member, role).queue(
                            success -> plugin.getLogger().info(
                                    "Донат-роль " + role.getName() + " снята с " + member.getUser().getAsTag()
                            ),
                            failure -> plugin.getLogger().warning(
                                    "Ошибка снятия донат-роли: " + failure.getMessage()
                            )
                    );
                }
            }
        }, error -> plugin.getLogger().warning(
                "Discord пользователь " + discordId + " не найден или не на сервере: " + error.getMessage()
        ));
    }


    public void syncStaffRole(UUID playerUUID, String highestStaffGroup) {
        if (rolesConfig == null || !rolesConfig.isStaffRole()) return;
        if (jda == null || !isRunning()) return;
        if (guildId == null || guildId.isEmpty()) return;

        String discordId = botIntegration.getDiscordId(playerUUID);
        if (discordId == null || discordId.isBlank()) return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        boolean isStaff = highestStaffGroup != null;

        guild.retrieveMemberById(discordId).queue(member -> {
            //универсальная роль для всего персонала
            String staffId = rolesConfig.getStaffId();
            if (staffId != null && !staffId.isEmpty()) {
                Role universalRole = guild.getRoleById(staffId);
                if (universalRole != null) {
                    boolean hasRole = member.getRoles().contains(universalRole);
                    if (isStaff && !hasRole) {
                        guild.addRoleToMember(member, universalRole).queue(
                                success -> plugin.getLogger().info(
                                        "Роль персонала " + universalRole.getName() + " выдана " + member.getUser().getAsTag()
                                ),
                                failure -> plugin.getLogger().warning(
                                        "Ошибка выдачи роли персонала: " + failure.getMessage()
                                )
                        );
                    } else if (!isStaff && hasRole) {
                        guild.removeRoleFromMember(member, universalRole).queue(
                                success -> plugin.getLogger().info(
                                        "Роль персонала " + universalRole.getName() + " снята с " + member.getUser().getAsTag()
                                ),
                                failure -> plugin.getLogger().warning(
                                        "Ошибка снятия роли персонала: " + failure.getMessage()
                                )
                        );
                    }
                }
            }

            for (String group : rolesConfig.getStaffGroups()) {
                String roleId = rolesConfig.getStaffRoleIdForGroup(group);
                if (roleId == null || roleId.isEmpty()) continue;

                Role role = guild.getRoleById(roleId);
                if (role == null) continue;

                boolean shouldHave = group.equalsIgnoreCase(highestStaffGroup);
                boolean hasRole = member.getRoles().contains(role);

                if (shouldHave && !hasRole) {
                    guild.addRoleToMember(member, role).queue(
                            success -> plugin.getLogger().info(
                                    "Роль должности " + role.getName() + " выдана " + member.getUser().getAsTag()
                            ),
                            failure -> plugin.getLogger().warning(
                                    "Ошибка выдачи роли должности: " + failure.getMessage()
                            )
                    );
                } else if (!shouldHave && hasRole) {
                    guild.removeRoleFromMember(member, role).queue(
                            success -> plugin.getLogger().info(
                                    "Роль должности " + role.getName() + " снята с " + member.getUser().getAsTag()
                            ),
                            failure -> plugin.getLogger().warning(
                                    "Ошибка снятия роли должности: " + failure.getMessage()
                            )
                    );
                }
            }
        }, error -> plugin.getLogger().warning(
                "Discord пользователь " + discordId + " не найден или не на сервере: " + error.getMessage()
        ));
    }
}