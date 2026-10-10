package z.caAuth.integration.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeClearEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.bots.discord.DiscordBot;
import z.caAuth.config.RolesConfig;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LuckPermsRoleListener {

    private final JavaPlugin plugin;
    private final Supplier<DiscordBot> discordBot;
    private final RolesConfig rolesConfig;
    private LuckPerms api;

    public LuckPermsRoleListener(JavaPlugin plugin, Supplier<DiscordBot> discordBot, RolesConfig rolesConfig) {
        this.plugin = plugin;
        this.discordBot = discordBot;
        this.rolesConfig = rolesConfig;
    }

    public void register() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.getLogger().warning("LuckPerms не найден, синхронизация ролей через LP отключена");
            return;
        }
        if (!rolesConfig.isDonateRole() && !rolesConfig.isStaffRole()) {
            return;
        }

        api = LuckPermsProvider.get();

        api.getEventBus().subscribe(plugin, NodeAddEvent.class,
                event -> handle(event.getTarget(), event.getNode()));
        api.getEventBus().subscribe(plugin, NodeRemoveEvent.class,
                event -> handle(event.getTarget(), event.getNode()));
        api.getEventBus().subscribe(plugin, NodeClearEvent.class,
                event -> handle(event.getTarget(), null));

        plugin.getLogger().info("LuckPerms интеграция ролей (донат/персонал) включена");
    }

    private void handle(PermissionHolder target, Node node) {
        if (node != null && !(node instanceof InheritanceNode)) return;
        if (!(target instanceof User user)) return;

        UUID uuid = user.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> sync(user, uuid));
    }

    private void sync(User user, UUID uuid) {
        DiscordBot bot = discordBot.get();
        if (bot == null || !bot.isRunning()) return;

        Set<String> currentGroups = user.getNodes(NodeType.INHERITANCE).stream()
                .filter(n -> !n.hasExpired())
                .map(n -> n.getGroupName().toLowerCase())
                .collect(Collectors.toSet());

        if (rolesConfig.isDonateRole()) {
            String tier = highestWeightGroup(currentGroups, rolesConfig.getDonateGroups());
            bot.getRoleService().syncDonateRole(uuid, tier);
        }
        if (rolesConfig.isStaffRole()) {
            String staffGroup = highestWeightGroup(currentGroups, rolesConfig.getStaffGroups());
            bot.getRoleService().syncStaffRole(uuid, staffGroup);
        }
    }

    private String highestWeightGroup(Set<String> currentGroups, Set<String> configuredGroups) {
        String best = null;
        int bestWeight = Integer.MIN_VALUE;

        for (String group : currentGroups) {
            if (!configuredGroups.contains(group)) continue;

            int weight = groupWeight(group);
            if (best == null || weight > bestWeight) {
                best = group;
                bestWeight = weight;
            }
        }
        return best;
    }

    private int groupWeight(String groupName) {
        Group group = api.getGroupManager().getGroup(groupName);
        if (group == null) return 0;
        return group.getWeight().orElse(0);
    }
}
