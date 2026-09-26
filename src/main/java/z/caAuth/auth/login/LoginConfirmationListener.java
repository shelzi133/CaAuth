package z.caAuth.auth.login;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LoginConfirmationListener implements Listener {
    private final LoginConfirmationManager confirmationManager;

    public LoginConfirmationListener(LoginConfirmationManager confirmationManager) {
        this.confirmationManager = confirmationManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        confirmationManager.requestConfirmation(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        confirmationManager.cancelPending(event.getPlayer().getUniqueId());
    }
}
