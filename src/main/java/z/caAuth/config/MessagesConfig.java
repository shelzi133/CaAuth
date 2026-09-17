package z.caAuth.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.util.ColorManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * МОИ любимые блять игровые сообщения, искрине их ненавижу блять
 */
public class MessagesConfig {
    private final JavaPlugin plugin;
    private final File configFile;
    private FileConfiguration config;
    
    public MessagesConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "messages.yml");
        loadConfig();
    }
    private void loadConfig() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try (InputStream defaultStream = plugin.getResource("messages.yml")) {
                if (defaultStream != null) {
                    Files.copy(defaultStream, configFile.toPath());
                } else {
                    configFile.createNewFile();
                    createDefaultConfig();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать конфиг сообщений: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    private void createDefaultConfig() {
    }
    public void reload() {
        loadConfig();
    }
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить конфиг сообщений: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getFirstJoin() {
        return getErrorPrefix() + colorize(config.getString("first-join", "Добро пожаловать на сервер Malanheim! Сейчас у тебя режим новичка, что его снять отыграй 6 часов или привяжи аккаунт к ТГ или ДС боту! /bind"));
    }

    public String getLoginConfirmationBotUnavailableKick() {
        return colorize(config.getString("login-confirm-unavailable-kick", "Бот не может подключиться. Если ошибка повторится, сообщите админам!"));
    }

    public String getPrefix() {
        return colorize(config.getString("prefix", " §f"));
    }
    
    public String getSuccessPrefix() {
        return colorize(config.getString("success-prefix", " §f"));
    }
    
    public String getErrorPrefix() {
        return colorize(config.getString("error-prefix", " §f"));
    }
    public String getAccountNotBound() {
        return getErrorPrefix() + colorize(config.getString("account-not-bound", "&7Твой аккаунт не привязан!"));
    }
    
    public String getAccountBoundDiscord() {
        return getSuccessPrefix() + colorize(config.getString("account-bound-discord", "Твой аккаунт привязан через дискорд"));
    }
    
    public String getAccountBoundTelegram() {
        return getSuccessPrefix() + colorize(config.getString("account-bound-telegram", "Твой аккаунт привязан через телеграм"));
    }
    
    public String getAccountAlreadyBound() {
        return getErrorPrefix() + colorize(config.getString("account-already-bound", "Твой аккаунт уже привязан!"));
    }
    
    public String getAccountBoundSuccessDiscord() {
        return getSuccessPrefix() + colorize(config.getString("account-bound-success-discord", "Твой аккаунт успешно привязан через Дискордик!"));
    }
    
    public String getAccountBoundSuccessTelegram() {
        return getSuccessPrefix() + colorize(config.getString("account-bound-success-telegram", "Твой аккаунт успешно привязан через Телеграм!"));
    }
    public String getBindingInstructions() {
        return colorize(config.getString("binding-instructions", "§eДля привязки аккаунта:"));
    }
    
    public String getBindingDiscord() {
        return colorize(config.getString("binding-discord", "§7-> Дс: зайдите на сервер Дискорд и напишите боту в Личку"));
    }
    
    public String getBindingTelegram() {
        return colorize(config.getString("binding-telegram", "§7-> Телега: напишите боту в тг. Юз"));
    }
    public String getCannotBreakBlocks() {
        return getErrorPrefix() + colorize(config.getString("cannot-break-blocks", "Ты не можешь ломать блоки без привязки аккаунта!"));
    }
    
    public String getCannotPlaceBlocks() {
        return getErrorPrefix() + colorize(config.getString("cannot-place-blocks", "Ты не можешь ставить блоки без привязки аккаунта!"));
    }
    
    public String getCannotInteract() {
        return getErrorPrefix() + colorize(config.getString("cannot-interact", "Ты не можешь взаимодействовать с объектами без привязки аккаунта!"));
    }
    
    public String getCannotInteractPlayers() {
        return getErrorPrefix() + colorize(config.getString("cannot-interact-players", "Ты не можешь взаимодействовать с игроками без привязки аккаунта!"));
    }
    
    public String getCannotAttack() {
        return getErrorPrefix() + colorize(config.getString("cannot-attack", "Ты не можешь атаковать без привязки аккаунта!"));
    }
    
    public String getCannotUseCommands() {
        return getErrorPrefix() + colorize(config.getString("cannot-use-commands", "Ты не можешь использовать команды без привязки аккаунта!"));
    }
    public String getCommandPlayerOnly() {
        return colorize(config.getString("command-player-only", "§cДурак блять?"));
    }
    
    public String getVerificationCodeGenerated(String code) {
        return getSuccessPrefix() + colorize(config.getString("verification-code-generated", "Ваш код верификации: §e%s")
                .replace("%s", code));
    }
    
    public String getVerificationCodeInstructions() {
        return colorize(config.getString("verification-code-instructions", "§7Отправьте код боту для завершения привязки"));
    }
    
    public String getCommandUsage() {
        return getErrorPrefix() + colorize(config.getString("command-usage", "/binding code - дает код"));
    }
    
    public String getNoPermission() {
        return getErrorPrefix() + colorize(config.getString("no-permission", "У вас нет прав на использование этой команды!"));
    }
    
    public String getConfigReloaded() {
        return getSuccessPrefix() + colorize(config.getString("config-reloaded", "Конфигурация перезагружена!"));
    }

    public String getPlayerNeverPlayed(String playerName) {
        String pattern = config.getString("player-never-played",
                "§cИгрок §e{player} §cникогда не играл на сервере!");
        return colorize(pattern.replace("{player}", playerName));
    }

    public String getCaauthUsageHeader() {
        return colorize(config.getString("caauth-usage-header", "§7Использование:"));
    }

    public String getCaauthUsageReload() {
        return colorize(config.getString("caauth-usage-reload", "§e/caauth reload §7- перезагрузить конфигурацию"));
    }

    public String getCaauthUsageInfo() {
        return colorize(config.getString("caauth-usage-info", "§e/caauth info <игрок> §7- информация об игроке"));
    }

    public String getCaauthInfoUsage() {
        return colorize(config.getString("caauth-info-usage", "§cИспользование: /caauth info <игрок>"));
    }

    public String getCaauthInfoTitle(String playerName) {
        String pattern = config.getString("caauth-info-title", "&6Информация об игроке: &e{player}");
        return colorize(pattern.replace("{player}", playerName));
    }

    public String getCaauthInfoSeparator() {
        return colorize(config.getString("caauth-info-separator", "§7------------------------------------------"));
    }

    public String getCaauthInfoUuid(String uuid) {
        String pattern = config.getString("caauth-info-uuid", "§7UUID: §f{uuid}");
        return colorize(pattern.replace("{uuid}", uuid));
    }

    public String getCaauthInfoNick(String nick) {
        String pattern = config.getString("caauth-info-nick", "§7Ник: §f{nick}");
        return colorize(pattern.replace("{nick}", nick));
    }

    public String getCaauthInfoIp(String ip) {
        String pattern = config.getString("caauth-info-ip", "§7IP адрес: §f{ip}");
        return colorize(pattern.replace("{ip}", ip));
    }

    public String getCaauthInfoClient(String client) {
        String pattern = config.getString("caauth-info-client", "§7Клиент: §f{client}");
        return colorize(pattern.replace("{client}", client));
    }

    public String getCaauthInfoPlaytime(String playtime) {
        String pattern = config.getString("caauth-info-playtime", "§7Время на сервере: §f{playtime}");
        return colorize(pattern.replace("{playtime}", playtime));
    }

    public String getCaauthInfoBinding(String binding) {
        String pattern = config.getString("caauth-info-binding", "§7Привязка аккаунта: {binding}");
        return colorize(pattern.replace("{binding}", binding));
    }

    public String getCaauthInfoIpUnavailable() {
        return colorize(config.getString("caauth-info-ip-unavailable", "§7недоступен §8(игрок не в сети)"));
    }

    public String getCaauthInfoClientUnavailable() {
        return colorize(config.getString("caauth-info-client-unavailable", "§7недоступен §8(игрок не в сети)"));
    }

    public String getCaauthInfoValueUnknown() {
        return colorize(config.getString("caauth-info-value-unknown", "§7неизвестен"));
    }

    public String getCaauthInfoPlaytimeUnknown() {
        return colorize(config.getString("caauth-info-playtime-unknown", "§7неизвестно"));
    }

    public String getCaauthInfoNotBound() {
        return colorize(config.getString("caauth-info-not-bound", "§cне привязан"));
    }

    public String getCaauthInfoBindingUnknownUser() {
        return colorize(config.getString("caauth-info-binding-unknown-user", "неизвестно"));
    }

    public String getCaauthBindingLabelDiscord() {
        return colorize(config.getString("caauth-binding-label-discord", "&#5865F2Discord"));
    }

    public String getCaauthBindingLabelTelegram() {
        return colorize(config.getString("caauth-binding-label-telegram", "&#26A5E4Telegram"));
    }

    public String getCaauthUnBindingUsage() {
        return colorize(config.getString("unbinding-usage", "/unbind ник"));
    }
    public String getCaauthUnBindingNoBound(String playerName) {
        String pattern = config.getString("unbinding-bound", "У игрока {player} аккаунт не привязан");
        return colorize(pattern.replace("{player}", playerName));
    }
    public String getCaauthUnBindingSuccess(String playerName) {
        String pattern = config.getString("unbinding-success", "Аккаунт игрока {player} успешно отвязан!");
        if (pattern == null) {
            pattern = "Аккаунт игрока {player} успешно отвязан!";
        }
        return colorize(pattern.replace("{player}", playerName));
    }
    public String getCaauthUnBindingType() {
        return colorize(config.getString("unbinding-type", "Тип привязки:"));
    }
    public String getCaauthUnBindingNotifyPlayer(String playerName) {
        String pattern = config.getString("unbinding-notify-player", "Ваш аккаунт был отвязан администратором!");
        return colorize(pattern.replace("{player}", playerName));
    }

    private String colorize(String text) {
        return ColorManager.colorize(text);
    }

    public String getLoginConfirmationSentDiscord(int timeoutSeconds) {
        String pattern = config.getString("login-confirmation-sent-discord",
                "&eДля продолжения подтвердите вход в Discord ({seconds} сек.)!");
        return getSuccessPrefix() + colorize(
                pattern.replace("{seconds}", String.valueOf(timeoutSeconds)));
    }

    public String getLoginConfirmationSentTelegram(int timeoutSeconds) {
        String pattern = config.getString("login-confirmation-sent-telegram",
                "&eДля продолжения подтвердите вход в Telegram ({seconds} сек.)!");
        return getSuccessPrefix() + colorize(
                pattern.replace("{seconds}", String.valueOf(timeoutSeconds)));
    }

    public String getLoginConfirmationConfirmed() {
        return getSuccessPrefix() + colorize(config.getString("login-confirmation-confirmed",
                "&aВход подтверждён, приятной игры!"));
    }

    public String getLoginConfirmationTimeoutKick() {
        return colorize(config.getString("login-confirmation-timeout-kick",
                "&cВы не подтвердили вход через бота за отведённое время. Зайдите снова и подтвердите вход."));
    }

    public String getLoginConfirmationDeniedKick() {
        return colorize(config.getString("login-confirmation-denied-kick",
                "&cВход был отклонён через бота. Если это были не вы - смените пароль от Discord/Telegram!"));
    }

}
