package z.caAuth.account;

import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RegisteredPlayerNames {
    private static final long TTL_MILLIS = 30_000L;

    private final Plugin plugin;
    private final AccountManager accountManager;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private volatile List<String> names = List.of();
    private volatile long loadedAt = 0L;

    public RegisteredPlayerNames(Plugin plugin, AccountManager accountManager) {
        this.plugin = plugin;
        this.accountManager = accountManager;
    }

    public Collection<String> get() {
        if (System.currentTimeMillis() - loadedAt > TTL_MILLIS) {
            refreshAsync();
        }
        return names;
    }

    public void refreshAsync() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                names = List.copyOf(accountManager.getRegisteredPlayerNames());
                loadedAt = System.currentTimeMillis();
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось обновить список ников: " + e);
            } finally {
                refreshing.set(false);
            }
        });
    }
}