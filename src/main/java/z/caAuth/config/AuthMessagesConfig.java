package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import z.caAuth.util.Colors;

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
    public Bots bots = new Bots();

    public static class Prefix extends OkaeriConfig {
        public boolean enabled = true;
        public String defaultPrefix = "&#FFCF4E⚠ &f";
        public String successPrefix = "&#98fb98&l✔ &f";
        public String errorPrefix = "&#FF7171&l⁉ &f";
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
        public String discord = "-> Дс: зайдите на сервер Дискорд и напишите боту в Личку";
        public String telegram = "-> Телега: напишите боту в тг. Юз";
    }

    public static class Command extends OkaeriConfig {
        public String playerOnly = "Дурак блять?";
        public String verificationCodeGenerated = "Ваш код верификации: %s";
        public String verificationCodeInstructions = "Отправьте код боту для завершения привязки";
        public String noPermission = "У вас нет прав на использование этой команды!";
        public String configReloaded = "Конфигурация перезагружена!";
        public String playerNeverPlayed = "Игрок {player} никогда не играл на сервере!";
    }

    public static class CaauthUsage extends OkaeriConfig {
        public String header = "Использование:";
        public String reload = "/caauth reload - перезагрузить конфигурацию";
        public String info = "/caauth info <игрок> - информация об игроке";
        public String discordHelp = "/caauth ds <start|stop|restart|info|online-reload> - управление Discord ботом";
        public String telegramHelp = "/caauth tg <start|stop|restart|info> - управление Telegram ботом";
    }

    public static class CaauthInfo extends OkaeriConfig {
        public String title = "Информация об игроке: {player}";
        public String separator = "------------------------------------------";
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
        public String bindingLabelDiscord = "Discord";
        public String bindingLabelTelegram = "Telegram";
    }

    public static class Unbinding extends OkaeriConfig {
        public String usage = "/unbind ник";
        public String noBound = "У игрока {player} аккаунт не привязан";
        public String success = "Аккаунт игрока {player} успешно отвязан!";
        public String type = "Тип привязки:";
        public String notifyPlayer = "Ваш аккаунт был отвязан администратором!";
    }

    public static class LoginConfirmation extends OkaeriConfig {
        public String sentDiscord = "Для продолжения подтвердите вход в Discord ({seconds} сек.)!";
        public String sentTelegram = "Для продолжения подтвердите вход в Telegram ({seconds} сек.)!";
        public String confirmed = "Вход подтверждён, приятной игры!";
        public String timeoutKick = "Вы не подтвердили вход через бота за отведённое время. Зайдите снова и подтвердите вход.";
        public String deniedKick = "Вход был отклонён через бота. Если это были не вы - смените пароль от Discord/Telegram!";
        public String waitForConfirmation = "Дождитесь подтверждения входа в боте.";
    }

    public static class ActivityPlayerRole extends OkaeriConfig {
        public String issuedSelf = "Роль активного игрока выдана!";
        public String takeSelf = "Роль активного игрока забрана!";
        public String issuedOther = "Роль активного игрока выдана игроку {player}!";
        public String takeOther = "Роль активного игрока забрана у игрока {player}!";
        public String botAvailable = "Бот не доступен";
    }

    public static class Bots extends OkaeriConfig {
        public String discordName = "Discord";
        public String telegramName = "Telegram";
        public String starting = "Запуск {bot} бота...";
        public String stopping = "Остановка {bot} бота...";
        public String restarting = "Перезапуск {bot} бота...";
        public String started = "{bot} бот запущен.";
        public String stopped = "{bot} бот остановлен.";
        public String restarted = "{bot} бот перезапущен.";
        public String alreadyRunning = "{bot} бот уже запущен.";
        public String notRunning = "{bot} бот не запущен.";
        public String disabledInConfig = "{bot} бот выключен в config.yml.";
        public String tokenMissing = "Токен {bot} бота не указан в config.yml.";
        public String busy = "С {bot} ботом уже выполняется другое действие, подождите.";
        public String failed = "Не удалось выполнить действие с {bot} ботом. См.консоль ";
        public String actionError = "Ошибка при выполнении действия с {bot} ботом: {error}";
        public String infoUsage = "Использование: /caauth {command} info <ник|ID>";
        public String infoSearching = "Поиск аккаунта {bot}...";
        public String infoNotFound = "Аккаунт {bot} {query} не привязан ни к одному игроку.";
        public String onlineReloaded = "Счётчик онлайна {bot} бота обновлён: {online}";
        public String onlineReloadFailed = "Не удалось обновить счётчик онлайна {bot} бота. См.консоль.";
        public String infoBotRequired = "{bot} бот не запущен, поиск по нику недоступен. Укажите числовой ID или запустите бота.";
    }

    public String getPrefix() {
        return colorize(prefix.enabled ? prefix.defaultPrefix : "");
    }

    public String getSuccessPrefix() {
        return colorize(prefix.enabled ? prefix.successPrefix : "");
    }

    public String getErrorPrefix() {
        return colorize(prefix.enabled ? prefix.errorPrefix : "");
    }

    public String getLoginConfirmationBotUnavailableKick() {
        return getErrorPrefix() + colorize(botError.loginConfirmUnavailableKick);
    }

    public String getModuleAuthDisabled() {
        return getErrorPrefix() + colorize(botError.moduleAuthDisabled);
    }

    public String getAccountNotBound() {
        return getErrorPrefix() + colorize(accountBound.accountNotBound);
    }

    public String getAccountBoundDiscord() {
        return getSuccessPrefix() + colorize(accountBound.accountBoundDiscord);
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
        return getPrefix() + colorize(binding.instructions);
    }

    public String getBindingDiscord() {
        return colorize(binding.discord);
    }

    public String getBindingTelegram() {
        return colorize(binding.telegram);
    }

    public String getCommandPlayerOnly() {
        return getErrorPrefix() + colorize(command.playerOnly);
    }

    public String getVerificationCodeGenerated(String code) {
        return getSuccessPrefix() + colorize(command.verificationCodeGenerated.replace("%s", code));
    }

    public String getVerificationCodeInstructions() {
        return getPrefix() + colorize(command.verificationCodeInstructions);
    }

    public String getNoPermission() {
        return getErrorPrefix() + colorize(command.noPermission);
    }

    public String getConfigReloaded() {
        return getSuccessPrefix() + colorize(command.configReloaded);
    }

    public String getPlayerNeverPlayed(String playerName) {
        return getErrorPrefix() + colorize(command.playerNeverPlayed.replace("{player}", playerName));
    }

    public String getCaauthUsageHeader() {
        return getPrefix() + colorize(caauthUsage.header);
    }

    public String getCaauthUsageReload() {
        return colorize(caauthUsage.reload);
    }

    public String getCaauthUsageInfo() {
        return colorize(caauthUsage.info);
    }

    public String getCaauthUsageDiscord() {
        return colorize(caauthUsage.discordHelp);
    }

    public String getCaauthUsageTelegram() {
        return colorize(caauthUsage.telegramHelp);
    }

    public String getCaauthInfoTitle(String playerName) {
        return getPrefix() + colorize(caauthInfo.title.replace("{player}", playerName));
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

    public String getCaauthUnbindingUsage() {
        return getErrorPrefix() + colorize(unbinding.usage);
    }

    public String getCaauthUnbindingNoBound(String playerName) {
        return getErrorPrefix() + colorize(unbinding.noBound.replace("{player}", playerName));
    }

    public String getCaauthUnbindingSuccess(String playerName) {
        return getSuccessPrefix() + colorize(unbinding.success.replace("{player}", playerName));
    }

    public String getCaauthUnbindingType() {
        return getPrefix() + colorize(unbinding.type);
    }

    public String getCaauthUnbindingNotifyPlayer(String playerName) {
        return getPrefix() + colorize(unbinding.notifyPlayer.replace("{player}", playerName));
    }

    public String getLoginConfirmationSentDiscord(int timeoutSeconds) {
        return getSuccessPrefix() + colorize(
                loginConfirmation.sentDiscord.replace("{seconds}", String.valueOf(timeoutSeconds)));
    }

    public String getLoginConfirmationSentTelegram(int timeoutSeconds) {
        return getSuccessPrefix() + colorize(
                loginConfirmation.sentTelegram.replace("{seconds}", String.valueOf(timeoutSeconds)));
    }

    public String getLoginConfirmationConfirmed() {
        return getSuccessPrefix() + colorize(loginConfirmation.confirmed);
    }

    public String getLoginConfirmationTimeoutKick() {
        return getErrorPrefix() + colorize(loginConfirmation.timeoutKick);
    }

    public String getLoginConfirmationDeniedKick() {
        return getErrorPrefix() + colorize(loginConfirmation.deniedKick);
    }

    public String getLoginConfirmationWait() {
        return getErrorPrefix() + colorize(loginConfirmation.waitForConfirmation);
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
        return getErrorPrefix() + colorize(activityPlayerRole.botAvailable);
    }

    public String getBotsDiscordName() {
        return colorize(bots.discordName);
    }

    public String getBotsTelegramName() {
        return colorize(bots.telegramName);
    }

    public String getBotsStarting(String bot) {
        return getPrefix() + botText(bots.starting, bot);
    }

    public String getBotsStopping(String bot) {
        return getPrefix() + botText(bots.stopping, bot);
    }

    public String getBotsRestarting(String bot) {
        return getPrefix() + botText(bots.restarting, bot);
    }

    public String getBotsStarted(String bot) {
        return getSuccessPrefix() + botText(bots.started, bot);
    }

    public String getBotsStopped(String bot) {
        return getSuccessPrefix() + botText(bots.stopped, bot);
    }

    public String getBotsRestarted(String bot) {
        return getSuccessPrefix() + botText(bots.restarted, bot);
    }

    public String getBotsAlreadyRunning(String bot) {
        return getErrorPrefix() + botText(bots.alreadyRunning, bot);
    }

    public String getBotsNotRunning(String bot) {
        return getErrorPrefix() + botText(bots.notRunning, bot);
    }

    public String getBotsDisabledInConfig(String bot) {
        return getErrorPrefix() + botText(bots.disabledInConfig, bot);
    }

    public String getBotsTokenMissing(String bot) {
        return getErrorPrefix() + botText(bots.tokenMissing, bot);
    }

    public String getBotsBusy(String bot) {
        return getErrorPrefix() + botText(bots.busy, bot);
    }

    public String getBotsFailed(String bot) {
        return getErrorPrefix() + botText(bots.failed, bot);
    }

    public String getBotsActionError(String bot, String error) {
        return getErrorPrefix() + colorize(bots.actionError
                .replace("{bot}", bot)
                .replace("{error}", String.valueOf(error)));
    }

    public String getBotsOnlineReloaded(String bot, int online) {
        return getSuccessPrefix() + colorize(bots.onlineReloaded
                .replace("{bot}", bot)
                .replace("{online}", String.valueOf(online)));
    }

    public String getBotsOnlineReloadFailed(String bot) {
        return getErrorPrefix() + botText(bots.onlineReloadFailed, bot);
    }

    public String getBotsInfoUsage(String command) {
        return getErrorPrefix() + colorize(bots.infoUsage.replace("{command}", command));
    }

    public String getBotsInfoSearching(String bot) {
        return getPrefix() + botText(bots.infoSearching, bot);
    }

    public String getBotsInfoNotFound(String bot, String query) {
        return getErrorPrefix() + colorize(bots.infoNotFound
                .replace("{bot}", bot)
                .replace("{query}", query));
    }

    public String getBotsInfoBotRequired(String bot) {
        return getErrorPrefix() + botText(bots.infoBotRequired, bot);
    }

    public boolean reload() {
        try {
            this.load(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String botText(String text, String bot) {
        return colorize(text.replace("{bot}", bot));
    }

    private String colorize(String text) {
        return Colors.colorize(text);
    }
}