package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;

import java.util.UUID;

@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class MainConfig extends OkaeriConfig {

    public Module module = new Module();

    public boolean reload() {
        try {
            this.load(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isVelocityMode() {
        return module.auth.enabled && module.auth.velocity.enabled;
    }

    public static class Module extends OkaeriConfig {
        public MainConfig.Auth auth = new MainConfig.Auth();
    }

    public static class Auth extends OkaeriConfig {
        @Comment("Вкл/выкл авторизацию через ботов, настройка в папке auth")
        public boolean enabled = true;

        @Comment("Работа через Velocity (tiAuth + PicoLimbo). Привязку и подтверждение входа проводит плагин CaAuthVelocity")
        public MainConfig.Velocity velocity = new MainConfig.Velocity();
    }

    public static class Velocity extends OkaeriConfig {
        @Comment("Вкл/выкл API для плагина CaAuthVelocity")
        public boolean enabled = true;
        @Comment("Адрес, на котором API принимает запросы от Velocity")
        public String host = "127.0.0.1";
        public int port = 25599;
        @Comment("Секретный ключ, такой же должен быть в config.yml плагина CaAuthVelocity")
        public String secret = UUID.randomUUID().toString();
    }

}