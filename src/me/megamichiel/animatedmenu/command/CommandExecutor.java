package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommandExecutor {
    
    private final List<CommandHandler> commands;
    
    public CommandExecutor(AnimatedMenuPlugin plugin, List<?> commands) {
        this.commands = new ArrayList<>();
        if (commands == null) return;
        for (Object o : commands) {
            if (o instanceof String || isPrimitiveWrapper(o)) {
                String str = String.valueOf(o);
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
                this.commands.add(getCommandHandler(cmd, plugin, str));
            }
        }
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
    
    public boolean isEmpty() {
        return commands.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public void execute(AnimatedMenuPlugin plugin, Player p) {
        for (CommandHandler handler : commands)
            if (!handler.execute(plugin, p))
                break;
    }
    
    private boolean isPrimitiveWrapper(Object input) {
        return input instanceof Integer || input instanceof Boolean
                || input instanceof Character || input instanceof Byte
                || input instanceof Short || input instanceof Double
                || input instanceof Long || input instanceof Float;
    }
    
    private interface CommandHandler {
        boolean execute(AnimatedMenuPlugin plugin, Player p);
    }
}
