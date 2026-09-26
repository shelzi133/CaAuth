package z.caAuth.auth.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BindingTabCompleter implements TabCompleter {
    private static final List<String> CODE = List.of("code");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String c : CODE) {
                if (c.toLowerCase(Locale.ROOT).startsWith(p)) {
                    out.add(c);
                }
            }
            return out;
        }
        return Collections.emptyList();
    }
}
