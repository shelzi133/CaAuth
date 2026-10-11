package z.caAuth.binding;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.config.MainConfig;
import z.caAuth.integration.velocity.VelocityClient;

import java.util.UUID;

public class PlayerKicker {

    private final JavaPlugin plugin;
    private final MainConfig mainConfig;
    private final VelocityClient velocityClient;

    public PlayerKicker(JavaPlugin plugin, MainConfig mainConfig, VelocityClient velocityClient) {
        this.plugin = plugin;
        this.mainConfig = mainConfig;
        this.velocityClient = velocityClient;
    }

    public void kick(UUID uuid, String name, String reason) {
        if (!mainConfig.isVelocityMode()) {
            kickLocal(uuid, reason);
            return;
        }
        velocityClient.kick(uuid, name, reason).thenAccept(kicked -> {
            if (!kicked) {
                kickLocal(uuid, reason);
            }
        });
    }

    private void kickLocal(UUID uuid, String reason) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.kickPlayer(reason);
            }
        });
    }
}
