package z.caAuth.auth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;

@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class AuthConfig extends OkaeriConfig {

    public Bots bots = new Bots();

    public boolean reload() {
        try {
            this.load(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static class Bots extends OkaeriConfig {
        public Discord discord = new Discord();
        public Telegram telegram = new Telegram();
        public LoginConfirmation loginConfirmation = new LoginConfirmation();
    }

    public static class Discord extends OkaeriConfig {
        public boolean enabled = false;
        public String token = "";
        @Comment("id ds сервера (gid) - опционально, для проверки участников сервера и выдачи роли")
        public String guildId = "";
    }

    public static class Telegram extends OkaeriConfig {
        public boolean enabled = false;
        public String token = "";
        public Proxy proxy = new Proxy();
    }

    public static class Proxy extends OkaeriConfig {
        @Comment(" ")
        @Comment("Если ТГ бот не работает из-за региональных ограничений (ебаный РКН)")
        public boolean enabled = false;
        @Comment("SOCKS5, SOCKS4 или HTTP")
        public String type = "SOCKS5";
        public String host = "127.0.0.1";
        public int port = 1080;
        public String username = "";
        public String password = "";
    }

    public static class LoginConfirmation extends OkaeriConfig {
        @Comment("Подтверждение входа")
        public boolean enabled = false;
        @Comment("Раз во сколько запрашивать, в часах")
        public int intervalHours = 6;
        @Comment("Сколько времени на подтверждение")
        public int timeoutSeconds = 60;

    }
}
