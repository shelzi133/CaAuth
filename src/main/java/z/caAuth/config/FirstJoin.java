package z.caAuth.config;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class FirstJoin implements Listener {

    private final MessagesConfig messagesConfig;

    public FirstJoin(MessagesConfig messagesConfig){
        this.messagesConfig = messagesConfig;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            player.sendMessage(messagesConfig.getFirstJoin());
        }
    }

}
