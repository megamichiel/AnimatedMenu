package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.Animatable;
import me.megamichiel.animationlib.config.AbstractConfig;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.BiPredicate;

import static java.util.Locale.ENGLISH;

public class CommandExecutor extends Animatable<List<BiPredicate<AnimatedMenuPlugin, Player>>> {

    private final AnimatedMenuPlugin plugin;
    
    public CommandExecutor(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    private <T, C> BiPredicate<AnimatedMenuPlugin, Player> getCommandHandler(Command<T, C> command,
                                                                             AnimatedMenuPlugin plugin, String str) {
        T val = command.parse(plugin, str);
        if (val == null) return null;
        C cached = command.tryCacheValue(plugin, val);
        return cached != null ?
                (plug, p) -> command.executeCached(plug, p, cached) :
                (plug, p) -> command.execute(plug, p, val);
    }
    
    private boolean isStringOrPrimitive(Object input) {
        return input instanceof String    || input instanceof Boolean ||
               input instanceof Character || input instanceof Number;
    }

    @Override
    protected List<BiPredicate<AnimatedMenuPlugin, Player>> convert(Nagger nagger, Object o) {
        List<BiPredicate<AnimatedMenuPlugin, Player>> result = new ArrayList<>();
        if (!(o instanceof List)) return result;
        BiPredicate<AnimatedMenuPlugin, Player> handler;
        for (Object raw : (List) o) {
            if (isStringOrPrimitive(raw)) {
                String str = raw.toString();
                Command<?, ?> cmd = getCommand(str.toLowerCase(ENGLISH));
                if (cmd.prefix != null)
                    str = str.substring(cmd.prefix.length() + 1).trim();
                if ((handler = getCommandHandler(cmd, plugin, str)) != null)
                    result.add(handler);
            } else if (raw instanceof AbstractConfig) {
                for (Map.Entry<String, Object> entry : ((AbstractConfig) raw).values().entrySet()) {
                    String name = entry.getKey().toLowerCase(ENGLISH);
                    Command<?, ?> command = plugin.getCommands().stream()
                            .filter(cmd -> name.equals(cmd.prefix.toLowerCase(ENGLISH)))
                            .findAny().orElse(null);
                    if (command == null) continue;
                    Object value = entry.getValue();
                    if (isStringOrPrimitive(value)) {
                        if ((handler = getCommandHandler(command, plugin, value.toString())) != null)
                            result.add(handler);
                    } else if (value instanceof List)
                        ((List<?>) value).stream().filter(this::isStringOrPrimitive)
                                .map(val -> getCommandHandler(command, plugin, val.toString()))
                                .filter(Objects::nonNull).forEach(result::add);
                }
            }
        }
        return result;
    }

    private Command getCommand(String name) {
        return plugin.getCommands().stream()
                .filter(cmd -> name.startsWith(cmd.prefix.toLowerCase(Locale.ENGLISH) + ':'))
                .findAny().orElseGet(DefaultCommand::new);
    }

    @Override
    protected Object getValue(Nagger nagger, AbstractConfig section, String key) {
        return section.getList(key);
    }

    public void execute(Player p) {
        List<BiPredicate<AnimatedMenuPlugin, Player>> commands = next();
        if (commands != null) for (BiPredicate<AnimatedMenuPlugin, Player> predicate : commands)
            if (!predicate.test(plugin, p))
                break;
    }
}
