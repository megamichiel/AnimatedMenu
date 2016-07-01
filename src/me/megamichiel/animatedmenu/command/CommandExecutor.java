package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.Animatable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommandExecutor extends Animatable<List<CommandExecutor.CommandHandler>> {

    private final AnimatedMenuPlugin plugin;
    
    public CommandExecutor(AnimatedMenuPlugin plugin,
                           ConfigurationSection section, String key) {
        this.plugin = plugin;
        load(plugin, section, key);
    }

    private <T, C> CommandHandler getCommandHandler(final Command<T, C> command,
                                                    AnimatedMenuPlugin plugin, String str) {
        final T val = command.parse(plugin, str);
        final C cached = command.tryCacheValue(plugin, val);
        return cached != null ? new CommandHandler() {
            @Override
            public boolean execute(AnimatedMenuPlugin plugin, Player p) {
                return command.executeCached(plugin, p, cached);
            }
        } : new CommandHandler() {
            @Override
            public boolean execute(AnimatedMenuPlugin plugin, Player p) {
                return command.execute(plugin, p, val);
            }
        };
    }
    
    private boolean isPrimitiveWrapper(Object input) {
        return input instanceof Integer || input instanceof Boolean
                || input instanceof Character || input instanceof Byte
                || input instanceof Short || input instanceof Double
                || input instanceof Long || input instanceof Float;
    }

    @Override
    protected List<CommandHandler> convert(Nagger nagger, Object o) {
        List<CommandHandler> result = new ArrayList<>();
        for (Object raw : (List) o) {
            if (raw instanceof String || isPrimitiveWrapper(raw)) {
                String str = raw.toString();
                Command cmd = null;
                String lowerStr = str.toLowerCase(Locale.US);
                for (Command command : plugin.getCommands()) {
                    if (lowerStr.startsWith(command.prefix.toLowerCase(Locale.US) + ":")) {
                        cmd = command;
                        str = str.substring(command.prefix.length() + 1).trim();
                        break;
                    }
                }
                if (cmd == null) cmd = new DefaultCommand();
                result.add(getCommandHandler(cmd, plugin, str));
            }
        }
        return result;
    }

    @Override
    protected Object getValue(Nagger nagger, ConfigurationSection section, String key) {
        return section.getList(key);
    }

    public void execute(AnimatedMenuPlugin plugin, Player p) {
        List<CommandHandler> commands = next();
        if (commands != null) for (CommandHandler handler : commands)
            if (!handler.execute(plugin, p))
                break;
    }

    interface CommandHandler {
        boolean execute(AnimatedMenuPlugin plugin, Player p);
    }
}
