package z.caAuth.bots.discord.role;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.binding.BindingService;
import z.caAuth.bots.discord.DiscordBot;
import z.caAuth.config.RolesConfig;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class DiscordRoleService {

    private final JavaPlugin plugin;
    private final BindingService bindingService;
    private final RolesConfig rolesConfig;
    private final DiscordBot accessor;

    public DiscordRoleService(JavaPlugin plugin, BindingService bindingService,
                              RolesConfig rolesConfig, DiscordBot accessor) {
        this.plugin = plugin;
        this.bindingService = bindingService;
        this.rolesConfig = rolesConfig;
        this.accessor = accessor;
    }

    public void assignRoleAndNicknameIfEnabled(String discordId, UUID playerUUID, String playerName) {
        Guild guild = resolveGuild();
        if (guild == null) return;

        guild.retrieveMemberById(discordId).queue(
                member -> {
                    applyBindingRole(guild, member);
                    applyBindingNickname(guild, member, playerName);
                },
                error -> logMemberNotFound(discordId, error)
        );
    }

    public CompletableFuture<Boolean> syncBinding(String discordId, String playerName) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Guild guild = resolveGuild();
        if (guild == null) {
            result.complete(false);
            return result;
        }
        guild.retrieveMemberById(discordId).queue(
                member -> {
                    applyBindingRole(guild, member);
                    applyBindingNickname(guild, member, playerName);
                    result.complete(true);
                },
                error -> {
                    logMemberNotFound(discordId, error);
                    result.complete(false);
                }
        );
        return result;
    }

    public void revokeBinding(String discordId) {
        Guild guild = resolveGuild();
        if (guild == null || discordId == null || discordId.isBlank()) return;

        guild.retrieveMemberById(discordId).queue(
                member -> {
                    if (rolesConfig != null) {
                        syncGroupRole(guild, member, rolesConfig.getRoleId(), false, "Роль привязки");
                        syncGroupRole(guild, member, rolesConfig.getActiveRoleId(), false, "Роль активного игрока");
                        syncGroupRole(guild, member, rolesConfig.getStaffId(), false, "Роль персонала");
                        for (String group : rolesConfig.getDonateGroups()) {
                            syncGroupRole(guild, member, rolesConfig.getRoleIdForGroup(group), false, "Донат-роль");
                        }
                        for (String group : rolesConfig.getStaffGroups()) {
                            syncGroupRole(guild, member, rolesConfig.getStaffRoleIdForGroup(group), false, "Роль должности");
                        }
                        if (rolesConfig.isChangeNicknameEnabled() && member.getNickname() != null) {
                            guild.modifyNickname(member, null).queue(
                                    success -> plugin.getLogger().info("Ник сброшен у " + member.getUser().getAsTag()),
                                    failure -> plugin.getLogger().warning("Ошибка сброса ника: " + failure.getMessage())
                            );
                        }
                    }
                },
                error -> logMemberNotFound(discordId, error)
        );
    }

    public void syncDonateRole(UUID playerUUID, String highestTier) {
        if (rolesConfig == null || !rolesConfig.isDonateRole()) return;
        withMember(playerUUID, (guild, member) -> {
            for (String group : rolesConfig.getDonateGroups()) {
                syncGroupRole(guild, member, rolesConfig.getRoleIdForGroup(group),
                        group.equalsIgnoreCase(highestTier), "Донат-роль");
            }
        });
    }

    public void syncActiveRole(UUID playerUUID, boolean shouldHaveRole) {
        if (rolesConfig == null || !rolesConfig.getActivePlayerRole()) return;
        String roleId = rolesConfig.getActiveRoleId();
        if (roleId == null || roleId.isEmpty()) return;

        withMember(playerUUID, (guild, member) ->
                syncGroupRole(guild, member, roleId, shouldHaveRole, "Роль активного игрока"));
    }

    public void syncStaffRole(UUID playerUUID, String highestStaffGroup) {
        if (rolesConfig == null || !rolesConfig.isStaffRole()) return;
        boolean isStaff = highestStaffGroup != null;

        withMember(playerUUID, (guild, member) -> {
            String staffId = rolesConfig.getStaffId();
            if (staffId != null && !staffId.isEmpty()) {
                syncGroupRole(guild, member, staffId, isStaff, "Роль персонала");
            }
            for (String group : rolesConfig.getStaffGroups()) {
                syncGroupRole(guild, member, rolesConfig.getStaffRoleIdForGroup(group),
                        group.equalsIgnoreCase(highestStaffGroup), "Роль должности");
            }
        });
    }

    private void applyBindingRole(Guild guild, Member member) {
        if (rolesConfig == null || !rolesConfig.isEnabled()) return;
        String roleId = rolesConfig.getRoleId();
        if (roleId == null || roleId.isEmpty()) return;

        Role role = guild.getRoleById(roleId);
        if (role == null || member.getRoles().contains(role)) return;

        guild.addRoleToMember(member, role).queue(
                success -> plugin.getLogger().info("Роль " + role.getName() + " выдана " + member.getUser().getAsTag()),
                failure -> plugin.getLogger().warning("Ошибка выдачи роли: " + failure.getMessage())
        );
    }

    private void applyBindingNickname(Guild guild, Member member, String playerName) {
        if (rolesConfig == null || !rolesConfig.isChangeNicknameEnabled()) return;

        String currentNickname = member.getEffectiveName();
        if (currentNickname.equals(playerName)) return;

        String nickname = playerName.length() > 32 ? playerName.substring(0, 32) : playerName;
        guild.modifyNickname(member, nickname).queue(
                success -> plugin.getLogger().info("Ник изменён на " + nickname),
                failure -> plugin.getLogger().warning("Ошибка смены ника: " + failure.getMessage())
        );
    }

    private void syncGroupRole(Guild guild, Member member, String roleId, boolean shouldHave, String logLabel) {
        if (roleId == null || roleId.isEmpty()) return;
        Role role = guild.getRoleById(roleId);
        if (role == null) return;

        boolean hasRole = member.getRoles().contains(role);
        if (shouldHave && !hasRole) {
            guild.addRoleToMember(member, role).queue(
                    success -> plugin.getLogger().info(logLabel + " " + role.getName() + " выдана " + member.getUser().getAsTag()),
                    failure -> plugin.getLogger().warning("Ошибка выдачи (" + logLabel + "): " + failure.getMessage())
            );
        } else if (!shouldHave && hasRole) {
            guild.removeRoleFromMember(member, role).queue(
                    success -> plugin.getLogger().info(logLabel + " " + role.getName() + " снята с " + member.getUser().getAsTag()),
                    failure -> plugin.getLogger().warning("Ошибка снятия (" + logLabel + "): " + failure.getMessage())
            );
        }
    }

    private void withMember(UUID playerUUID, BiConsumer<Guild, Member> action) {
        Guild guild = resolveGuild();
        if (guild == null) return;

        String discordId = bindingService.getDiscordId(playerUUID);
        if (discordId == null || discordId.isBlank()) return;

        guild.retrieveMemberById(discordId).queue(
                member -> action.accept(guild, member),
                error -> logMemberNotFound(discordId, error)
        );
    }

    private Guild resolveGuild() {
        if (!accessor.isRunning() || accessor.getJda() == null) return null;
        String guildId = accessor.getGuildId();
        if (guildId == null || guildId.isEmpty()) return null;
        return accessor.getJda().getGuildById(guildId);
    }

    private void logMemberNotFound(String discordId, Throwable error) {
        plugin.getLogger().warning("Discord пользователь " + discordId + " не найден или не на сервере: " + error.getMessage());
    }
}
