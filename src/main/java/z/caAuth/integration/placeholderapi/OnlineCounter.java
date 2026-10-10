package z.caAuth.integration.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class OnlineCounter {

    private static final String VANISH_PLACEHOLDER = "%vanish_playercount%";

    private final Plugin plugin;

    public OnlineCounter(Plugin plugin) {
        this.plugin = plugin;
    }

    public int count() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            return 0;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Player viewer = players.stream()
                    .filter(p -> !p.hasPermission("vanish.show"))
                    .findFirst()
                    .orElse(players.get(0));
            try {
                String parsed = PlaceholderAPI.setPlaceholders(viewer, VANISH_PLACEHOLDER);
                if (parsed != null && !parsed.equals(VANISH_PLACEHOLDER)) {
                    return Integer.parseInt(parsed.trim());
                }
            } catch (NumberFormatException ignored) {
            } catch (Throwable t) {
                plugin.getLogger().warning("Ошибка плейсхолдера " + VANISH_PLACEHOLDER + ": " + t);
            }
        }

        return countVisiblePlayers(players);
    }

    private int countVisiblePlayers(List<Player> players) {
        long hidden = players.stream()
                .filter(p -> players.stream().anyMatch(viewer -> viewer != p && !viewer.canSee(p)))
                .count();
        return (int) (players.size() - hidden);
    }
}
