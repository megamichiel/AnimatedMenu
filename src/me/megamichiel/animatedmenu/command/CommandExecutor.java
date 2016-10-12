package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.Animatable;
import me.megamichiel.animationlib.config.AbstractConfig;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class CommandExecutor extends Animatable<List<CommandExecutor.CommandHandler>> {

    private final AnimatedMenuPlugin plugin;
    
    public CommandExecutor(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    private <T, C> CommandHandler getCommandHandler(Command<T, C> command,
                                                    AnimatedMenuPlugin plugin, String str) {
        T val = command.parse(plugin, str);
        C cached = command.tryCacheValue(plugin, val);
        return cached != null ?
                (plug, p) -> command.executeCached(plug, p, cached) :
                (plug, p) -> command.execute(plug, p, val);
    }
    
    private boolean isStringOrPrimitive(Object input) {
        return input instanceof String || input instanceof Boolean
                || input instanceof Character || input instanceof Number;
    }

    @Override
    protected List<CommandHandler> convert(Nagger nagger, Object o) {
        List<CommandHandler> result = new ArrayList<>();
        if (!(o instanceof List)) return result;
        for (Object raw : (List) o) {
            if (isStringOrPrimitive(raw)) {
                String str = raw.toString();
                Command cmd = getCommand(str.toLowerCase(Locale.US));
                if (cmd.prefix != null)
                    str = str.substring(cmd.prefix.length() + 1).trim();
                result.add(getCommandHandler(cmd, plugin, str));
            } else if (raw instanceof AbstractConfig) {
                for (Map.Entry<String, Object> entry : ((AbstractConfig) raw).values().entrySet()) {
                    String name = entry.getKey().toLowerCase(Locale.US);
                    Command command = plugin.getCommands().stream()
                            .filter(cmd -> name.equals(cmd.prefix.toLowerCase(Locale.US)))
                            .findAny().orElse(null);
                    if (command == null) continue;
                    Object value = entry.getValue();
                    if (isStringOrPrimitive(value))
                        result.add(getCommandHandler(command, plugin, value.toString()));
                    else if (value instanceof List) {
                        Stream<CommandHandler> stream = ((List<?>) value).stream()
                                .filter(this::isStringOrPrimitive)
                                .map(val -> getCommandHandler(command, plugin, val.toString()));
                        stream.forEach(result::add);
                    }
                }
            }
        }
        return result;
    }

    private Command getCommand(String name) {
        return plugin.getCommands().stream()
                .filter(cmd -> name.startsWith(cmd.prefix.toLowerCase(Locale.US) + ':'))
                .findAny().orElseGet(DefaultCommand::new);
    }

    @Override
    protected Object getValue(Nagger nagger, AbstractConfig section, String key) {
        return section.getList(key);
    }

    public void execute(Player p) {
        List<CommandHandler> commands = next();
        if (commands != null) for (CommandHandler handler : commands)
            if (!handler.execute(plugin, p))
                break;
    }

    interface CommandHandler {
        boolean execute(AnimatedMenuPlugin plugin, Player p);
    }
}
