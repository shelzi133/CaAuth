package z.caAuth.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class SkinsConfig extends OkaeriConfig {

    @Comment("Сколько секунд ждать ввод ника, ссылки или PNG-файла")
    public int inputTimeoutSeconds = 120;

    @Comment("Максимальный размер PNG-файла в байтах")
    public int maxPngBytes = 65536;

    @Comment("Слово, которым можно отменить ввод ника или ссылки в игровом чате")
    public String cancelWord = "отмена";

    public Gui gui = new Gui();

    public Messages messages = new Messages();

    public Labels labels = new Labels();

    public boolean reload() {
        try {
            this.load(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static class Gui extends OkaeriConfig {
        @Comment("Название меню профилей")
        public String title = "&8Скины";
        @Comment("Название меню выбора способа установки")
        public String typeTitle = "&8Способ установки скина";
        @Comment("Номера ячеек (от 0) для 7 профилей в меню 9x5")
        public List<Integer> profileSlots = List.of(10, 11, 12, 13, 14, 15, 16);
        @Comment("Заглушки: material - материал, slots - номера ячеек (от 0). Можно добавить свои filter_N")
        public Map<String, Filter> filters = defaultFilters();
        @Comment("Голова пустого профиля. Строка base-head=<значение текстуры> ставит голову с этой текстурой, иначе - материал из Bukkit")
        public EmptyProfile emptyProfile = new EmptyProfile();
        @Comment("{slot} - номер профиля")
        public String profileName = "&fПрофиль &e{slot}";
        public List<String> profileEmptyLore = List.of(
                "&7Пусто, отображается белая голова",
                "",
                "&aЛКМ или ПКМ &7- установить скин");
        @Comment("{input} - ник, ссылка или PNG, {source} - тип скина")
        public List<String> profileFilledLore = List.of(
                "&7Скин: &f{input}",
                "&7Источник: &f{source}",
                "",
                "&aЛКМ &7- надеть скин",
                "&eПКМ &7- сменить скин",
                "&cShift+ПКМ &7- удалить скин");
        @Comment("Номер ячейки способа установки: по нику")
        public int typeNickSlot = 11;
        public String typeNickName = "&aПо нику";
        public String typeNickMaterial = "NAME_TAG";
        public List<String> typeNickLore = List.of("&7Введите ник игрока в чат");
        @Comment("Номер ячейки способа установки: по ссылке")
        public int typeUrlSlot = 13;
        public String typeUrlName = "&bПо ссылке";
        public String typeUrlMaterial = "COMPASS";
        public List<String> typeUrlLore = List.of("&7Введите ссылку на PNG-скин в чат");
        @Comment("Номер ячейки способа установки: через бота")
        public int typeBotSlot = 15;
        public String typeBotName = "&dЧерез бота";
        public String typeBotMaterial = "BOOK";
        public List<String> typeBotLore = List.of("&7Загрузите PNG-файл в боте");
        @Comment("Номер ячейки кнопки назад в меню способов")
        public int typeBackSlot = 22;
        public String backName = "&7Назад";
        public String backMaterial = "ARROW";
    }

    public static class Filter extends OkaeriConfig {
        public String material = "GRAY_STAINED_GLASS_PANE";
        public List<Integer> slots = List.of();
    }

    public static class EmptyProfile extends OkaeriConfig {
        public String material = "base-head=eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI4ODU4MmJiYzJkN2QzMTA5NmU3NDBjMzVlYjY5MDBjMDdmOGY5YTJhNjM5YjNlMjZmNzk0MTc2YWMyNDhlYyJ9fX0=";
    }

    private static Map<String, Filter> defaultFilters() {
        Map<String, Filter> filters = new LinkedHashMap<>();
        Filter first = new Filter();
        first.material = "PINK_STAINED_GLASS_PANE";
        first.slots = List.of(0, 44);
        filters.put("filter_1", first);
        Filter second = new Filter();
        second.material = "WHITE_STAINED_GLASS_PANE";
        second.slots = List.of(1, 7, 9, 17, 27, 35, 37, 43);
        filters.put("filter_2", second);
        return filters;
    }

    public static class Messages extends OkaeriConfig {
        public String playerOnly = "Эту команду может использовать только игрок!";
        public String unavailable = "Скины сейчас недоступны, нужен плагин &#FFC0E4SkinsRestorer&f.";
        @Comment("{slot} - номер профиля")
        public String wearSuccess = "Скин профиля &#FFC0E4{slot} &fнадет.";
        public String wearEmpty = "Профиль &#FFC0E4{slot} &fпуст, сначала установите скин.";
        public String wearError = "Не удалось надеть скин. Попробуйте позже.";
        public String deleted = "Скин удалён из профиля &#FFC0E4{slot}&f.";
        public String deleteEmpty = "Профиль &#FFC0E4{slot} &fуже пуст.";
        @Comment("{cancel} - слово отмены, {seconds} - время ожидания")
        public String promptNick = "Введите ник игрока в чат. Отмена: &#FFC0E4{cancel}&f. Время: &#FFC0E4{seconds} сек&f.";
        public String promptUrl = "Введите ссылку на PNG-скин в чат. Отмена: &#FFC0E4{cancel}&f. Время: &#FFC0E4{seconds} сек&f.";
        @Comment("Через бота: бот отправил владельцу аккаунта сообщение, теперь нужно прислать PNG-файл. {slot} - номер профиля")
        public String promptBot = "Бот написал вам в личные сообщения. Отправьте PNG-файл для профиля &#FFC0E4{slot}&f.";
        public String botUnavailable = "Бот сейчас недоступен, загрузка PNG через бота невозможна.";
        public String cancelled = "Ввод скина отменён.";
        public String timeout = "Время ввода истекло. Откройте &#FFC0E4/skins &fзаново.";
        public String invalidNick = "Неверный ник. Допустимы латинские буквы, цифры и _, от 3 до 16 символов.";
        public String invalidUrl = "Неверная ссылка. Нужна ссылка, начинающаяся с &#FF7083http:// &fили &#FF7083https://&f.";
        public String notFound = "Ник &#FF7083{input} &fне найден.";
        public String playerNotFound = "Игрок &#FF7083{player} &fникогда не заходил на сервер.";
        public String setSuccess = "Скин сохранён в профиль &#FFC0E4{slot}&f. Наденьте его ЛКМ по профилю.";
        public String saveError = "Не удалось сохранить скин. Попробуйте позже.";
    }

    public static class Labels extends OkaeriConfig {
        public String sourceNick = "ник";
        public String sourceUrl = "ссылка";
        public String sourcePng = "PNG-файл";
        public String emptyName = "Пусто";
    }
}
