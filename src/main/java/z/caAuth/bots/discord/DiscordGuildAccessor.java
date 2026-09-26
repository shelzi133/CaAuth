package z.caAuth.bots.discord;

import net.dv8tion.jda.api.JDA;

public interface DiscordGuildAccessor {
    JDA getJda();
    boolean isRunning();
    String getGuildId();
}
