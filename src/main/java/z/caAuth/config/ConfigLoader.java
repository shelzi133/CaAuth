package z.caAuth.config;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static <T extends OkaeriConfig> T load(JavaPlugin plugin, Class<T> type, String path) {
        File file = new File(plugin.getDataFolder(), path);
        File folder = file.getParentFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        try {
            return ConfigManager.create(type, it -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(file);
                it.withRemoveOrphans(false);
                it.saveDefaults();
                it.load(true);
            });
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось загрузить " + path + ": " + e.getMessage(), e);
        }
    }
}
