package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandExecutor {
    
    private final List<CommandHandler> commands;
    
    public CommandExecutor(AnimatedMenuPlugin plugin, List<?> commands) {
        this.commands = new ArrayList<>();
        if (commands == null) return;
        for (Object o : commands) {
            if (o instanceof String || isPrimitiveWrapper(o)) {
                String str = String.valueOf(o);
                Command cmd = null;
                for (Command command : plugin.getCommands()) {
                    if (str.toLowerCase().startsWith(command.prefix.toLowerCase() + ":")) {
                        cmd = command;
                        str = str.substring(command.prefix.length() + 1).trim();
                        break;
                    }
                }
                if (cmd == null) {
                    cmd = new DefaultCommand();
                }
                final Command command = cmd;
                final Object val = cmd.parse(plugin, str);
                final Object cached = cmd.tryCacheValue(plugin, val);
                if (cached != null) {
                    this.commands.add(new CommandHandler() {
                        @Override
                        public boolean execute(AnimatedMenuPlugin plugin, Player p) {
                            return command.executeCached(plugin, p, cached);
                        }
                    });
                } else {
                    this.commands.add(new CommandHandler() {
                        @Override
                        public boolean execute(AnimatedMenuPlugin plugin, Player p) {
                            return command.execute(plugin, p, val);
                        }
                    });
                }
            }
        }
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
