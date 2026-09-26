package z.caAuth.auth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import z.caAuth.auth.util.ColorManager;

@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class AuthMessagesConfig extends OkaeriConfig {

    public Prefix prefix = new Prefix();
    public BotError botError = new BotError();
    public AccountBound accountBound = new AccountBound();
    public Binding binding = new Binding();
    public Command command = new Command();
    public CaauthUsage caauthUsage = new CaauthUsage();
    public CaauthInfo caauthInfo = new CaauthInfo();
    public Unbinding unbinding = new Unbinding();
    public LoginConfirmation loginConfirmation = new LoginConfirmation();
    public ActivityPlayerRole activityPlayerRole = new ActivityPlayerRole();

    public static class Prefix extends OkaeriConfig {
        public boolean enabled = true;
        public String defaultPrefix = "";
        public String successPrefix = "";
        public String errorPrefix = "";
    }

    public static class BotError extends OkaeriConfig {
        public String loginConfirmUnavailableKick = "Бот не может подключиться. Если ошибка повторится, сообщите админам!";
        public String moduleAuthDisabled = "Модуль auth выключен";
    }

    public static class AccountBound extends OkaeriConfig {
        public String accountNotBound = "Твой аккаунт не привязан!";
        public String accountBoundDiscord = "Твой аккаунт привязан через дискорд";
        public String accountBoundTelegram = "Твой аккаунт привязан через телеграм";
        public String accountAlreadyBound = "Твой аккаунт уже привязан!";
        public String accountBoundSuccessDiscord = "Твой аккаунт успешно привязан через Дискордик!";
        public String accountBoundSuccessTelegram = "Твой аккаунт успешно привязан через Телеграм!";
    }

    public static class Binding extends OkaeriConfig {
        public String instructions = "Для привязки аккаунта:";
        public String discord = "§7-> Дс: зайдите на сервер Дискорд и напишите боту в Личку";
        public String telegram = "§7-> Телега: напишите боту в тг. Юз";
    }


    public static class Command extends OkaeriConfig {
        public String playerOnly = "§cДурак блять?";
        public String verificationCodeGenerated = "Ваш код верификации: §e%s";
        public String verificationCodeInstructions = "§7Отправьте код боту для завершения привязки";
        public String usage = "/binding code - дает код";
        public String noPermission = "У вас нет прав на использование этой команды!";
        public String configReloaded = "Конфигурация перезагружена!";
        public String playerNeverPlayed = "§cИгрок §e{player} §cникогда не играл на сервере!";
    }

    public static class CaauthUsage extends OkaeriConfig {
        public String header = "Использование:";
        public String reload = "/caauth reload §7- перезагрузить конфигурацию";
        public String info = "/caauth info <игрок> §7- информация об игроке";
    }

    public static class CaauthInfo extends OkaeriConfig {
        public String usage = "Использование: /caauth info <игрок>";
        public String title = "Информация об игроке: {player}";
        public String separator = "§7------------------------------------------";
        public String uuid = "UUID: {uuid}";
        public String nick = "Ник: {nick}";
        public String ip = "IP адрес: {ip}";
        public String client = "Клиент: {client}";
        public String playtime = "Время на сервере: {playtime}";
        public String binding = "Привязка аккаунта: {binding}";
        public String ipUnavailable = "недоступен (игрок не в сети)";
        public String clientUnavailable = "недоступен (игрок не в сети)";
        public String valueUnknown = "неизвестен";
        public String playtimeUnknown = "неизвестно";
        public String notBound = "не привязан";
        public String bindingUnknownUser = "неизвестно";
        public String bindingLabelDiscord = "&#5865F2Discord";
        public String bindingLabelTelegram = "&#26A5E4Telegram";
    }

    public static class Unbinding extends OkaeriConfig {
        public String usage = "/unbind ник";
        public String noBound = "У игрока {player} аккаунт не привязан";
        public String success = "Аккаунт игрока {player} успешно отвязан!";
        public String type = "Тип привязки:";
        public String notifyPlayer = "Ваш аккаунт был отвязан администратором!";
    }

    public static class LoginConfirmation extends OkaeriConfig {
        public String sentDiscord = "&eДля продолжения подтвердите вход в Discord ({seconds} сек.)!";
        public String sentTelegram = "&eДля продолжения подтвердите вход в Telegram ({seconds} сек.)!";
        public String confirmed = "&aВход подтверждён, приятной игры!";
        public String timeoutKick = "&cВы не подтвердили вход через бота за отведённое время. Зайдите снова и подтвердите вход.";
        public String deniedKick = "&cВход был отклонён через бота. Если это были не вы - смените пароль от Discord/Telegram!";
    }

    public static class ActivityPlayerRole extends OkaeriConfig {
        public String issuedSelf = "Роль активного игрока выдана!";
        public String takeSelf = "Роль активного игрока забрана!";

        public String issuedOther = "Роль активного игрока выдана игроку {player}!";
        public String takeOther = "Роль активного игрока забрана у игрока {player}!";

        public String botAvailable = "Бот не доступен";

    }


    public String getPrefix() {
        return  colorize(prefix.enabled ? prefix.defaultPrefix : "");
    }

    public String getSuccessPrefix() {
        return colorize(prefix.enabled ? prefix.successPrefix : "");
    }

    public String getErrorPrefix() {
        return colorize(prefix.enabled ? prefix.errorPrefix : "");
    }

    public String getLoginConfirmationBotUnavailableKick() {
        return colorize(botError.loginConfirmUnavailableKick);
    }

    public String getModuleAuthDisabled(){return colorize(botError.moduleAuthDisabled);}

    public String getAccountNotBound() {
        return getErrorPrefix() + colorize(accountBound.accountNotBound);
    }

    public String getAccountBoundDiscord() {
        return getPrefix() + colorize(accountBound.accountBoundDiscord);
    }

    public String getAccountBoundTelegram() {
        return getSuccessPrefix() + colorize(accountBound.accountBoundTelegram);
    }

    public String getAccountAlreadyBound() {
        return getErrorPrefix() + colorize(accountBound.accountAlreadyBound);
    }

    public String getAccountBoundSuccessDiscord() {
        return getSuccessPrefix() + colorize(accountBound.accountBoundSuccessDiscord);
    }

    public String getAccountBoundSuccessTelegram() {
        return getSuccessPrefix() + colorize(accountBound.accountBoundSuccessTelegram);
    }

    public String getBindingInstructions() {
        return colorize(binding.instructions);
    }

    public String getBindingDiscord() {
        return colorize(binding.discord);
    }

    public String getBindingTelegram() {
        return colorize(binding.telegram);
    }

    public String getCommandPlayerOnly() {
        return colorize(command.playerOnly);
    }

    public String getVerificationCodeGenerated(String code) {
        return getSuccessPrefix() + colorize(command.verificationCodeGenerated.replace("%s", code));
    }

    public String getVerificationCodeInstructions() {
        return colorize(command.verificationCodeInstructions);
    }

    public String getCommandUsage() {
        return getErrorPrefix() + colorize(command.usage);
    }

    public String getNoPermission() {
        return getErrorPrefix() + colorize(command.noPermission);
    }

    public String getConfigReloaded() {
        return getSuccessPrefix() + colorize(command.configReloaded);
    }

    public String getPlayerNeverPlayed(String playerName) {
        return colorize(command.playerNeverPlayed.replace("{player}", playerName));
    }

    public String getCaauthUsageHeader() {
        return colorize(caauthUsage.header);
    }

    public String getCaauthUsageReload() {
        return colorize(caauthUsage.reload);
    }

    public String getCaauthUsageInfo() {
        return colorize(caauthUsage.info);
    }

    public String getCaauthInfoUsage() {
        return colorize(caauthInfo.usage);
    }

    public String getCaauthInfoTitle(String playerName) {
        return colorize(caauthInfo.title.replace("{player}", playerName));
    }

    public String getCaauthInfoSeparator() {
        return colorize(caauthInfo.separator);
    }

    public String getCaauthInfoUuid(String uuid) {
        return colorize(caauthInfo.uuid.replace("{uuid}", uuid));
    }

    public String getCaauthInfoNick(String nick) {
        return colorize(caauthInfo.nick.replace("{nick}", nick));
    }

    public String getCaauthInfoIp(String ip) {
        return colorize(caauthInfo.ip.replace("{ip}", ip));
    }

    public String getCaauthInfoClient(String client) {
        return colorize(caauthInfo.client.replace("{client}", client));
    }

    public String getCaauthInfoPlaytime(String playtime) {
        return colorize(caauthInfo.playtime.replace("{playtime}", playtime));
    }

    public String getCaauthInfoBinding(String binding) {
        return colorize(caauthInfo.binding.replace("{binding}", binding));
    }

    public String getCaauthInfoIpUnavailable() {
        return colorize(caauthInfo.ipUnavailable);
    }

    public String getCaauthInfoClientUnavailable() {
        return colorize(caauthInfo.clientUnavailable);
    }

    public String getCaauthInfoValueUnknown() {
        return colorize(caauthInfo.valueUnknown);
    }

    public String getCaauthInfoPlaytimeUnknown() {
        return colorize(caauthInfo.playtimeUnknown);
    }

    public String getCaauthInfoNotBound() {
        return colorize(caauthInfo.notBound);
    }

    public String getCaauthInfoBindingUnknownUser() {
        return colorize(caauthInfo.bindingUnknownUser);
    }

    public String getCaauthBindingLabelDiscord() {
        return colorize(caauthInfo.bindingLabelDiscord);
    }

    public String getCaauthBindingLabelTelegram() {
        return colorize(caauthInfo.bindingLabelTelegram);
    }

    public String getCaauthUnBindingUsage() {
        return colorize(unbinding.usage);
    }

    public String getCaauthUnBindingNoBound(String playerName) {
        return colorize(unbinding.noBound.replace("{player}", playerName));
    }

    public String getCaauthUnBindingSuccess(String playerName) {
        return colorize(unbinding.success.replace("{player}", playerName));
    }

    public String getCaauthUnBindingType() {
        return colorize(unbinding.type);
    }

    public String getCaauthUnBindingNotifyPlayer(String playerName) {
        return colorize(unbinding.notifyPlayer.replace("{player}", playerName));
    }

    public String getLoginConfirmationSentDiscord(int timeoutSeconds) {
        return getSuccessPrefix() + colorize(
                loginConfirmation.sentDiscord.replace("{seconds}", String.valueOf(timeoutSeconds)));
    }

    public String getLoginConfirmationSentTelegram(int timeoutSeconds) {
        return getSuccessPrefix() + colorize(
                loginConfirmation.sentTelegram.replace("{seconds}", String.valueOf(timeoutSeconds)));
    }

    public String getIssuedActivityPlayerRoleSelf() {
        return getSuccessPrefix() + colorize(activityPlayerRole.issuedSelf);
    }

    public String getTakeActivityPlayerRoleSelf() {
        return getSuccessPrefix() + colorize(activityPlayerRole.takeSelf);
    }

    public String getIssuedActivityPlayerRoleOther(String playerName) {
        return getSuccessPrefix() + colorize(activityPlayerRole.issuedOther.replace("{player}", playerName));
    }

    public String getTakeActivityPlayerRoleOther(String playerName) {
        return getSuccessPrefix() + colorize(activityPlayerRole.takeOther.replace("{player}", playerName));
    }

    public String getBotUnavailable() {
        return colorize(activityPlayerRole.botAvailable);
    }


    public String getLoginConfirmationConfirmed() {
        return getSuccessPrefix() + colorize(loginConfirmation.confirmed);
    }

    public String getLoginConfirmationTimeoutKick() {
        return colorize(loginConfirmation.timeoutKick);
    }

    public String getLoginConfirmationDeniedKick() {
        return colorize(loginConfirmation.deniedKick);
    }

    public boolean reload() {
        try {
            this.load(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String colorize(String text) {
        return ColorManager.colorize(text);
    }
}