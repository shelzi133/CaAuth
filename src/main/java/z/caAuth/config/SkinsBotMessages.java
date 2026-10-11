package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;

import java.util.List;

@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class SkinsBotMessages extends OkaeriConfig {

    public Skins skins = new Skins();

    @Comment("Банк (нужен CalistoBanking): {card} - номер карты, {balance} - баланс, {recipient} - ник получателя, {amount} - сумма, {fee} - комиссия, {percent} - процент комиссии, {total} - сумма списания")
    public Bank bank = new Bank();

    public static class Bank extends OkaeriConfig {
        public String menuButton = "💳 Банк";
        public String overviewTitle = "**💳 Банк**\n\nКарта: `{card}`\nБаланс: **{balance} АР**";
        public String noCard = "❌ У вас нет банковской карты. Получите её на сервере в банкомате.";
        public String unavailable = "❌ Банк сейчас недоступен.";
        public String transferButton = "💸 Перевести АР";
        public String transferPrompt = "**💸 Перевод АР**\n\nОтправьте одним сообщением ник или номер карты получателя и сумму, например:\n`Steve 100`\n`1234-5678-9012-3456 100`";
        public String invalidFormat = "❌ Формат: ник или номер карты, пробел, сумма. Например: `Steve 100`";
        public String modalTitle = "Перевод АР";
        public String modalRecipientLabel = "Ник или номер карты";
        public String modalRecipientPlaceholder = "Steve или 1234-5678-9012-3456";
        public String modalAmountLabel = "Сумма (АР)";
        public String modalAmountPlaceholder = "100";
        public String confirmText = "**💸 Подтвердите перевод**\n\nПолучатель: **{recipient}** (`{card}`)\nСумма: **{amount} АР**\nКомиссия: **{fee} АР** ({percent}%)\nБудет списано: **{total} АР**";
        public String confirmYesButton = "✅ Перевести";
        public String confirmNoButton = "❌ Отмена";
        public String success = "✅ Переведено **{amount} АР** игроку **{recipient}**.\nКомиссия: **{fee} АР**. Остаток: **{balance} АР**.";
        public String cancelled = "❌ Перевод отменён.";
        public String recipientNotFound = "❌ Получатель не найден. Укажите ник игрока с картой или номер карты.";
        public String ownCard = "❌ Нельзя переводить АР самому себе.";
        public String invalidAmount = "❌ Сумма должна быть целым числом от 1 до 1000000.";
        public String insufficient = "❌ Недостаточно АР. Нужно **{total} АР** (с комиссией), на карте **{balance} АР**.";
        public String transferError = "❌ Не удалось выполнить перевод. Попробуйте позже.";
        public String discordMenuStyle = "SUCCESS";
        public String discordTransferStyle = "PRIMARY";
        public String discordYesStyle = "SUCCESS";
        public String discordNoStyle = "DANGER";
    }

    public boolean reload() {
        try {
            this.load(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static class Skins extends OkaeriConfig {

        public Bot bot = new Bot();

        public DiscordButtons discordButtons = new DiscordButtons();

        public Layout layout = new Layout();

        public static class Bot extends OkaeriConfig {
            public String menuButton = "🎭 Скины";
            public String accountTitle = "**🎭 Скины**\n\nВыберите аккаунт, на который будет надет скин:";
            public String accountNotFound = "❌ Аккаунт не найден. Откройте «Скины» заново.";
            @Comment("{account} - ник аккаунта")
            public String hubTitle = "**🎭 Скины: {account}**\n\nВыберите профиль. Пустой профиль отображается белой головой.";
            @Comment("{slot} - номер профиля, {name} - ник, ссылка или PNG")
            public String hubProfileButton = "{slot} · {name}";
            public String profileTitle = "**Профиль {slot}**";
            public String profileFilled = "Скин: **{input}**\nИсточник: **{source}**\n\nНадеть скин можно в игре командой /skins, ЛКМ по профилю.";
            public String profileEmpty = "Профиль пуст. Установите скин, и он появится здесь.";
            public String editButton = "✏️ Изменить";
            public String setButton = "➕ Установить";
            public String deleteButton = "🗑 Удалить";
            public String deleteYesButton = "✅ Да, удалить";
            public String deleteNoButton = "❌ Отмена";
            public String backButton = "⬅️ Назад";
            public String hubButton = "🎭 К скинам";
            public String openMenuButton = "📋 В меню";
            public String typeTitle = "**Профиль {slot}**\n\nВыберите способ установки скина:";
            public String typeNickButton = "👤 По нику";
            public String typeUrlButton = "🔗 По ссылке";
            public String typePngButton = "🖼 PNG-файл";
            public String nickModalTitle = "Скин по нику";
            public String nickModalLabel = "Ник игрока";
            public String nickModalPlaceholder = "Например, Notch";
            public String urlModalTitle = "Скин по ссылке";
            public String urlModalLabel = "Ссылка на PNG";
            public String urlModalPlaceholder = "https://...";
            @Comment("{slot} - номер профиля, {seconds} - время ожидания")
            public String pngPrompt = "**Профиль {slot}**\n\nОтправьте PNG-файл (64×64 или 64×32) следующим сообщением в личку боту, в течение **{seconds} сек.**\nВ Telegram отправляйте файлом, а не фото, иначе изображение сожмётся.";
            @Comment("Telegram: подсказка перед вводом ника, {seconds} - время ожидания")
            public String telegramNickPrompt = "**Введите ник игрока** следующим сообщением, в течение **{seconds} сек.**\nНик: латинские буквы, цифры и _.";
            @Comment("Telegram: подсказка перед вводом ссылки, {seconds} - время ожидания")
            public String telegramUrlPrompt = "**Введите ссылку на PNG-скин** следующим сообщением, в течение **{seconds} сек.**";
            public String pngWaitHint = "Отправьте PNG-файл вложением.";
            public String deleteConfirm = "⚠️ Удалить скин из профиля **{slot}**? Профиль станет пустым.";
            public String deleted = "✅ Скин удалён из профиля **{slot}**.";
            public String saved = "✅ Скин сохранён в профиль **{slot}**.\n\nНадеть его можно в игре командой /skins, ЛКМ по профилю.";
            public String invalidNick = "❌ Неверный ник. Допустимы латинские буквы, цифры и _, от 3 до 16 символов.";
            public String invalidUrl = "❌ Неверная ссылка. Нужна ссылка, начинающаяся с https://.";
            public String invalidPng = "❌ Это не PNG-файл скина. Размер должен быть 64×64 или 64×32.";
            @Comment("{max} - максимум в килобайтах")
            public String tooLarge = "❌ Файл слишком большой. Максимум: {max} КБ.";
            public String notFound = "❌ Ник **{input}** не найден.";
            public String saveError = "❌ Не удалось сохранить скин. Попробуйте позже.";
            public String unavailable = "❌ Скины сейчас недоступны.";
            public String notBound = "❌ Ваш аккаунт не привязан к игроку. Отправьте /start, чтобы привязать его.";
            public String expired = "⏳ Время ожидания истекло. Начните заново через меню.";
        }
    
        public static class DiscordButtons extends OkaeriConfig {
            @Comment("Кнопка «Скины» в главном меню")
            public String menu = "PRIMARY";
            @Comment("Кнопки профилей")
            public String profile = "SECONDARY";
            @Comment("Кнопки изменения и выбора способа")
            public String action = "PRIMARY";
            @Comment("Кнопки удаления")
            public String delete = "DANGER";
            @Comment("Подтверждение удаления")
            public String confirm = "SUCCESS";
            @Comment("Кнопки назад и отмены")
            public String back = "SECONDARY";
        }

        public static class Layout extends OkaeriConfig {
            @Comment("Discord, главное меню: кнопки по порядку, в одном ряду. Доступно: manage, skins")
            public List<String> discordMain = List.of("manage", "skins", "twink", "bank");
            @Comment("Discord, меню управления аккаунтом: кнопки по порядку. Доступно: unbind, password, twink, back")
            public List<String> discordManage = List.of("unbind", "password", "sync", "back");
            @Comment("Discord, главное меню: сколько кнопок в ряду (1-5)")
            public int discordMainPerRow = 5;
            @Comment("Discord, меню управления аккаунтом: сколько кнопок в ряду (1-5)")
            public int discordManagePerRow = 5;
            @Comment("Discord, скины: сколько профилей в ряду (1-5)")
            public int discordSkinsProfilesPerRow = 5;
            @Comment("Discord, скины: сколько аккаунтов в ряду при выборе (1-5)")
            public int discordSkinsAccountsPerRow = 5;
            @Comment("Telegram, главное меню: кнопки по порядку. Доступно: manage, skins")
            public List<String> telegramMain = List.of("manage", "skins", "twink", "bank");
            @Comment("Telegram, главное меню: сколько кнопок в ряду (1-5)")
            public int telegramMainPerRow = 2;
            @Comment("Telegram, меню управления аккаунтом: кнопки по порядку. Доступно: unbind, password, twink, back")
            public List<String> telegramManage = List.of("unbind", "password", "back");
            @Comment("Telegram, меню управления аккаунтом: сколько кнопок в ряду (1-5)")
            public int telegramManagePerRow = 1;
            @Comment("Telegram, скины: сколько профилей в ряду (1-5)")
            public int telegramSkinsProfilesPerRow = 2;
            @Comment("Telegram, скины: сколько аккаунтов в ряду при выборе (1-5)")
            public int telegramSkinsAccountsPerRow = 1;
        }
    }
}
