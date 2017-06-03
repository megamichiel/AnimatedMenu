package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.Animatable;
import me.megamichiel.animationlib.config.AbstractConfig;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static java.util.Locale.ENGLISH;

public class CommandExecutor extends Animatable<List<BiPredicate<AnimatedMenuPlugin, Player>>> implements Consumer<Player> {

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
        for (Object raw : (List) o) {
            if (isStringOrPrimitive(raw)) {
                String str = raw.toString();
                int index = str.indexOf(':');
                Command<?, ?> cmd;
                if (index <= 0) cmd = TextCommand.DEFAULT;
                else if ((cmd = plugin.findCommand(str.substring(0, index))) == TextCommand.DEFAULT) {
                    nagger.nag("No command with id '" + str.substring(0, index) + "' found! Defaulting to player command");
                }
                if (!cmd.prefix.isEmpty()) {
                    str = str.substring(cmd.prefix.length() + 1).trim();
                }
                BiPredicate<AnimatedMenuPlugin, Player> handler = getCommandHandler(cmd, plugin, str);
                if (handler != null) result.add(handler);
            } else if (raw instanceof AbstractConfig) {
                ((AbstractConfig) raw).forEach((key, value) -> {
                    Command<?, ?> command = plugin.findCommand(key.toLowerCase(ENGLISH));
                    if (command == TextCommand.DEFAULT) return;
                    if (isStringOrPrimitive(value)) {
                        BiPredicate<AnimatedMenuPlugin, Player> handler = getCommandHandler(command, plugin, value.toString());
                        if (handler != null) {
                            result.add(handler);
                        }
                    } else if (value instanceof List) {
                        ((List<?>) value).stream().filter(this::isStringOrPrimitive)
                                .map(val -> getCommandHandler(command, plugin, val.toString()))
                                .filter(Objects::nonNull).forEach(result::add);
                    }
                });
            }
        }
        return result;
    }

    @Override
    protected Object getValue(Nagger nagger, AbstractConfig section, String key) {
        return section.getList(key);
    }

    @Override
    public void accept(Player player) {
        List<BiPredicate<AnimatedMenuPlugin, Player>> commands = next();
        if (commands != null) {
            for (BiPredicate<AnimatedMenuPlugin, Player> predicate : commands) {
                if (!predicate.test(plugin, player)) {
                    break;
                }
            }
        }
    }
}
