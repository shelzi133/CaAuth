package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;


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

    public static class Module extends OkaeriConfig {
        public MainConfig.Auth auth = new MainConfig.Auth();
        public MainConfig.Skins skins = new MainConfig.Skins();
    }

    public static class Auth extends OkaeriConfig {
        @Comment("Вкл/выкл авторизацию через ботов, настройка в папке auth")
        public boolean enabled = true;
    }

    public static class Skins extends OkaeriConfig {
        @Comment("Вкл/выкл систему скинов. (Нужен SkinRestorer)")
        public boolean enabled = false;
    }

}
