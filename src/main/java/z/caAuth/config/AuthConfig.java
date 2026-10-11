package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;

@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class AuthConfig extends OkaeriConfig {

    public Bots bots = new Bots();

    public Proxy proxy = new Proxy();

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
        public BindingCode bindingCode = new BindingCode();
        public PasswordChange passwordChange = new PasswordChange();
        public Twink twink = new Twink();
    }

    public static class Twink extends OkaeriConfig {
        @Comment("Сколько твинков можно привязать к одному аккаунту (включая ожидающих код)")
        public int maxCount = 2;
        @Comment("Сколько минут действует код привязки твинка")
        public int inviteLifetimeMinutes = 15;
    }

    public static class PasswordChange extends OkaeriConfig {
        @Comment("Длина пароля, который бот выдаёт при смене пароля (минимум 8)")
        public int length = 16;
    }

    public static class BindingCode extends OkaeriConfig {
        @Comment("Сколько минут действует код из /binding code")
        public int lifetimeMinutes = 10;
        @Comment("Сколько неверных кодов можно отправить боту до блокировки. 0 - без ограничений")
        public int maxAttempts = 5;
        @Comment("За какой период считаются неверные коды, в минутах")
        public int attemptWindowMinutes = 10;
        @Comment("На сколько минут бот перестаёт принимать коды от пользователя после превышения попыток")
        public int lockMinutes = 10;
    }

    public static class Discord extends OkaeriConfig {
        public boolean enabled = false;
        public String token = "";
        @Comment("id ds сервера (gid) - опционально, для проверки участников сервера и выдачи роли")
        public String guildId = "";
        @Comment("Коэффициент на который будет умножен онлайн в описании дс бота")
        public int fakeOnline = 1;
    }

    public static class Telegram extends OkaeriConfig {
        public boolean enabled = false;
        public String token = "";
    }

    public static class Proxy extends OkaeriConfig {
        @Comment("Если бот не работает из-за региональных ограничений (ебаный РКН)")
        public boolean enabled = false;
        @Comment("Для какого бота работает прокси: telegram, ds или all")
        public String typeBot = "telegram";
        @Comment("SOCKS5, SOCKS4 или HTTP")
        public String type = "SOCKS5";
        public String host = "127.0.0.1";
        public int port = 1080;
        public String username = "";
        public String password = "";
        @Comment("Сколько раз повторять запуск бота, если прокси ещё недоступен (например, Velocity ещё грузится). 0 - не повторять")
        public int retryAttempts = 6;
        @Comment("Пауза между повторами, в секундах")
        public int retryDelaySeconds = 10;
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
