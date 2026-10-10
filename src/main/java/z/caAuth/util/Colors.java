package z.caAuth.util;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Colors {

    private Colors() {
    }

    private static final Pattern HEX_AMP_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static final Pattern COLOR_TAG_PATTERN =
            Pattern.compile("\\[COLOR=#([A-Fa-f0-9]{6})](.*?)\\[/COLOR]", Pattern.CASE_INSENSITIVE);

    private static final Pattern HEX_BRACE_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})}");

    private static final Pattern ANY_HEX_PATTERN = Pattern.compile(
            "&#([A-Fa-f0-9]{6})|\\[COLOR=#([A-Fa-f0-9]{6})]|\\{#([A-Fa-f0-9]{6})}",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PLAIN_AMPERSAND = Pattern.compile("&(?!#[A-Fa-f0-9]{6})");

    private static final Pattern HEX_MARKER_PATTERN =
            Pattern.compile("&#([A-Fa-f0-9]{6})|\\{#([A-Fa-f0-9]{6})}");

    public static String convertColorCodes(String text) {
        if (text == null) {
            return "";
        }
        return PLAIN_AMPERSAND.matcher(text).replaceAll("§");
    }

    public static boolean containsHexColors(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return ANY_HEX_PATTERN.matcher(text).find();
    }

    public static String fixHex(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        text = fixColorTags(text);
        text = fixHexBraces(text);
        text = fixHexAmp(text);
        return text;
    }

    private static String toMinecraftHex(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (int i = 0; i < hex.length(); i++) {
            sb.append('§').append(hex.charAt(i));
        }
        return sb.toString();
    }

    private static String fixColorTags(String text) {
        Matcher matcher = COLOR_TAG_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String content = matcher.group(2);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(toMinecraftHex(hex) + content));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String fixHexBraces(String text) {
        Matcher matcher = HEX_BRACE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(toMinecraftHex(hex)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String fixHexAmp(String text) {
        Matcher matcher = HEX_AMP_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(toMinecraftHex(hex)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        String result = text;
        if (containsHexColors(result)) {
            result = fixHex(result);
        }
        return convertColorCodes(result);
    }
    public static void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(colorize(message));
    }

    public static String extractHexColor(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = ANY_HEX_PATTERN.matcher(text);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    return matcher.group(i);
                }
            }
        }
        return null;
    }
    public static int hexToInt(String hex) {
        if (hex == null || hex.isEmpty()) {
            return -1;
        }
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String processDiscordColors(String text) {
        if (text == null) {
            return "";
        }
        String result = text;
        result = COLOR_TAG_PATTERN.matcher(result).replaceAll("$2");
        result = HEX_BRACE_PATTERN.matcher(result).replaceAll("");
        result = HEX_AMP_PATTERN.matcher(result).replaceAll("");
        return result;
    }

    public static String processTelegramColors(String text) {
        if (text == null) {
            return "";
        }
        String result = wrapColorTagsHtml(text);
        result = wrapHexMarkersHtml(result);
        return result;
    }

    private static String wrapColorTagsHtml(String text) {
        Matcher matcher = COLOR_TAG_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String content = matcher.group(2);
            String replacement = "<span style=\"color:#" + hex + "\">" + content + "</span>";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String wrapHexMarkersHtml(String text) {
        Matcher matcher = HEX_MARKER_PATTERN.matcher(text);
        List<MatchResult> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.toMatchResult());
        }

        if (matches.isEmpty()) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        int lastPos = 0;
        boolean inColor = false;

        for (MatchResult match : matches) {
            sb.append(text, lastPos, match.start());

            if (inColor) {
                sb.append("</span>");
                inColor = false;
            }

            String hex = match.group(1) != null ? match.group(1) : match.group(2);
            sb.append("<span style=\"color:#").append(hex).append("\">");
            inColor = true;

            lastPos = match.end();
        }

        sb.append(text.substring(lastPos));
        if (inColor) {
            sb.append("</span>");
        }

        return sb.toString();
    }
}