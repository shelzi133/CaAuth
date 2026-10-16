package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;

import java.util.ArrayList;
import java.util.List;
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
        public MainConfig.Skins skins = new MainConfig.Skins();
        public MainConfig.TiAuth tiauth = new MainConfig.TiAuth();
        public MainConfig.Banking banking = new MainConfig.Banking();
    }

    public static class TiAuth extends OkaeriConfig {
        @Comment("Вкл/выкл функции tiAuth: смена пароля и твинки. Если выключено, кнопок в ботах нет. Работает только вместе с module.auth.velocity")
        public boolean enabled = false;
    }

    public static class Banking extends OkaeriConfig {
        @Comment("Вкл/выкл банк: кнопка и переводы в ботах. Нужен плагин CalistoBanking")
        public boolean enabled = false;
    }

    public static class Skins extends OkaeriConfig {
        @Comment("Вкл/выкл скины: команда /caauth skins, кнопки в ботах. Нужен плагин SkinsRestorer, настройка в папке skins")
        public boolean enabled = false;
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
        @Comment("Входящий API плагина CaAuthVelocity: кик игрока при отвязке и смена пароля в tiAuth. Должен совпадать с inbound в его config.yml")
        public ProxyApi proxyApi = new ProxyApi();
        @Comment("Дополнительные прокси (если их несколько и они независимы). Формат каждого элемента: host, port, timeout-seconds. Кик и смена пароля отправляются на все прокси")
        public List<ProxyApi> additionalProxyApis = new ArrayList<>();
    }

    public static class ProxyApi extends OkaeriConfig {
        public String host = "127.0.0.1";
        public int port = 25600;
        @Comment("Сколько секунд ждать ответ Velocity")
        public int timeoutSeconds = 10;
    }

}