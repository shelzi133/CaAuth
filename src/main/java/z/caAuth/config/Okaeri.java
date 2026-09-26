package z.caAuth.config;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.auth.config.AuthConfig;
import z.caAuth.auth.config.AuthMessagesConfig;
import z.caAuth.bots.discord.Role.config.RolesConfig;

import java.io.File;

public class Okaeri {

    private AuthConfig config;
    private AuthMessagesConfig messages;
    private MainConfig mainConfig;
    private RolesConfig rolesConfig;


    public MainConfig loadMainConfig(JavaPlugin plugin) {
        try {
            mainConfig = ConfigManager.create(MainConfig.class, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(plugin.getDataFolder(), "config.yml"));
                it.saveDefaults();
                it.load(true);
            });
            return mainConfig;
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось загрузить config.yml: " + e.getMessage());
            e.printStackTrace();
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
    }

    public AuthConfig loadConfig(JavaPlugin plugin) {
        try {
            File folder = new File(plugin.getDataFolder(), "auth");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File file = new File(folder, "auth-config.yml");

            config = ConfigManager.create(AuthConfig.class, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(file);
                it.saveDefaults();
                it.load(true);
            });
            return config;
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось загрузить auth-config.yml: " + e.getMessage());
            e.printStackTrace();
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
    }

    public AuthMessagesConfig loadMessages(JavaPlugin plugin) {
        try {
            File folder = new File(plugin.getDataFolder(), "auth");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File file = new File(folder, "auth-messages.yml");

            messages = ConfigManager.create(AuthMessagesConfig.class, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(file);
                it.saveDefaults();
                it.load(true);
            });
            return messages;
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось загрузить auth-messages.yml: " + e.getMessage());
            e.printStackTrace();
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
    }
    public RolesConfig loadRolesConfig(JavaPlugin plugin) {
        try {
            File folder = new File(plugin.getDataFolder(), "bots");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File file = new File(folder, "roles.yml");

            rolesConfig = ConfigManager.create(RolesConfig.class, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(file);
                it.saveDefaults();
                it.load(true);
            });
            return rolesConfig;
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось загрузить roles.yml: " + e.getMessage());
            e.printStackTrace();
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
    }

}