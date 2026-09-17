package z.caAuth.bots.Discord.Role;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class RolesConfig {
    private final JavaPlugin plugin;
    private final File configFile;
    private FileConfiguration config;
    
    public RolesConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        File botsFolder = new File(plugin.getDataFolder(), "bots");
        this.configFile = new File(botsFolder, "roles.yml");
        loadConfig();
    }
    private void loadConfig() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();

            try (InputStream defaultStream = plugin.getResource("bots/roles.yml")) {
                if (defaultStream != null) {
                    Files.copy(defaultStream, configFile.toPath());
                } else {
                    configFile.createNewFile();
                    createDefaultConfig();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать конфиг ролей: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    private void createDefaultConfig() {
        config.set("enabled", false);
        config.set("role-id", "");
        config.set("donate-role", false);
        config.set("donate-groups.imba", "");
        config.set("donate-groups.slay", "");
        config.set("donate-groups.prime", "");
        config.set("staff-role", false);
        config.set("staff-id", "");
        config.set("staff-groups.helper", "");
        config.set("staff-groups.moder", "");
        config.set("staff-groups.padmin", "");
        config.set("change-nickname", false);
        save();
    }
    public void reload() {
        loadConfig();
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить конфиг ролей: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean isEnabled() {
        return config.getBoolean("enabled", false);
    }

    public String getRoleId() {
        return config.getString("role-id", "");
    }

    public boolean isDonateRole() {return config.getBoolean("donate-role", false);}


    public java.util.Set<String> getDonateGroups() {
        return groupNames("donate-groups");
    }


    public String getRoleIdForGroup(String group) {
        return roleIdFor("donate-groups", group);
    }

    public boolean isStaffRole() {return config.getBoolean("staff-role", false);}


    public String getStaffId() {return config.getString("staff-id", "");}


    public java.util.Set<String> getStaffGroups() {
        return groupNames("staff-groups");
    }

    public String getStaffRoleIdForGroup(String group) {
        return roleIdFor("staff-groups", group);
    }

    private java.util.Set<String> groupNames(String section) {
        org.bukkit.configuration.ConfigurationSection configSection = config.getConfigurationSection(section);
        if (configSection == null) {
            return java.util.Collections.emptySet();
        }
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        for (String key : configSection.getKeys(false)) {
            result.add(key.toLowerCase());
        }
        return result;
    }

    private String roleIdFor(String section, String group) {
        if (group == null || group.isBlank()) return "";
        return config.getString(section + "." + group.toLowerCase(), "");
    }

    public boolean isChangeNicknameEnabled() {
        return config.getBoolean("change-nickname", false);
    }
}

