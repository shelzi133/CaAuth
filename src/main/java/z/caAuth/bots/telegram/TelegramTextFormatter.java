package z.caAuth.bots.telegram;

import z.caAuth.auth.util.ColorManager;

public final class TelegramTextFormatter {

    private TelegramTextFormatter() {
    }

    public static String convertMarkdownToHtml(String text) {
        if (text == null) {
            return "";
        }
        text = ColorManager.processTelegramColors(text);
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        text = text.replaceAll("```py\\s*(.+?)\\s*```", "<code>$1</code>");
        text = text.replaceAll("```(.+?)```", "<code>$1</code>");
        return text;
    }

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
