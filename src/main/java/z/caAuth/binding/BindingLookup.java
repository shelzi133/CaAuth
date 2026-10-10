package z.caAuth.binding;

import z.caAuth.account.AccountManager;
import z.caAuth.account.BindingType;
import z.caAuth.bots.BotManager;
import z.caAuth.bots.ChatBot;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BindingLookup {

    private final AccountManager accountManager;
    private final BotManager botManager;

    public BindingLookup(AccountManager accountManager, BotManager botManager) {
        this.accountManager = accountManager;
        this.botManager = botManager;
    }

    public UUID findByBindingId(BindingType type, String bindingId) {
        return accountManager.getPlayerByBinding(type, bindingId);
    }

    public UUID findByDisplayName(BindingType type, String query) {
        Map<UUID, String> bindings = accountManager.getBindingsByType(type);
        for (Map.Entry<UUID, String> entry : bindings.entrySet()) {
            String display = resolveDisplay(type, entry.getValue());
            if (display != null && matchesName(display, query)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String resolveDisplay(BindingType type, String bindingId) {
        ChatBot bot = botManager.getBot(type);
        return bot != null ? bot.resolveDisplay(bindingId) : null;
    }

    private boolean matchesName(String display, String query) {
        String q = query.toLowerCase(Locale.ROOT);
        String d = display.toLowerCase(Locale.ROOT).trim();
        if (d.equals(q) || d.equals("@" + q)) {
            return true;
        }
        for (String token : d.split("[\s()|]+")) {
            if (token.startsWith("@") && token.substring(1).equals(q)) {
                return true;
            }
        }
        return false;
    }
}
