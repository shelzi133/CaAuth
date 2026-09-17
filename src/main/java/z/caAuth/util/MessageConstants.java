package z.caAuth.util;

public final class MessageConstants {
    private MessageConstants() {
    }
    public static final String PREFIX = " §f";
    public static final String SUCCESS_PREFIX = " §f";
    public static final String ERROR_PREFIX = " §f";
    public static final String ACCOUNT_NOT_BOUND = ERROR_PREFIX + "Твой аккаунт не привязан!";
    public static final String ACCOUNT_BOUND_DISCORD = SUCCESS_PREFIX + "Твой аккаунт привязан через дискорд";
    public static final String ACCOUNT_BOUND_TELEGRAM = SUCCESS_PREFIX + "Твой аккаунт привязан через телеграм";
    public static final String ACCOUNT_ALREADY_BOUND = ERROR_PREFIX + "Твой аккаунт уже привязан!";
    public static final String ACCOUNT_BOUND_SUCCESS_DISCORD = SUCCESS_PREFIX + "Твой аккаунт успешно привязан через Дискордик!";
    public static final String ACCOUNT_BOUND_SUCCESS_TELEGRAM = SUCCESS_PREFIX + "Твой аккаунт успешно привязан через Телеграм!";
    public static final String BINDING_INSTRUCTIONS = "§eДля привязки аккаунта:";
    public static final String BINDING_DISCORD = "§7-> Дс: зайдите на сервер Дискорд и напишите боту в Личку";
    public static final String BINDING_TELEGRAM = "§7-> Телега: напишите боту в тг. Юз";
    public static final String CANNOT_BREAK_BLOCKS = ERROR_PREFIX + "Ты не можешь ломать блоки без привязки аккаунта!";
    public static final String CANNOT_PLACE_BLOCKS = ERROR_PREFIX + "Ты не можешь ставить блоки без привязки аккаунта!";
    public static final String CANNOT_INTERACT = ERROR_PREFIX + "Ты не можешь взаимодействовать с объектами без привязки аккаунта!";
    public static final String CANNOT_INTERACT_PLAYERS = ERROR_PREFIX + "Ты не можешь взаимодействовать с игроками без привязки аккаунта!";
    public static final String CANNOT_ATTACK = ERROR_PREFIX + "Ты не можешь атаковать без привязки аккаунта!";
    public static final String CANNOT_USE_COMMANDS = ERROR_PREFIX + "Ты не можешь использовать команды без привязки аккаунта!";
    public static final String COMMAND_PLAYER_ONLY = "§cДурак блять?";
    public static final String VERIFICATION_CODE_GENERATED = SUCCESS_PREFIX + "Ваш код верификации: §e%s";
    public static final String VERIFICATION_CODE_INSTRUCTIONS = "§7Отправьте код боту для завершения привязки";
    public static final String COMMAND_USAGE = ERROR_PREFIX + "/binding code - дает код";
}

