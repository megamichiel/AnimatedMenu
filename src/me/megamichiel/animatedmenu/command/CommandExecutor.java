package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.AbsAnimatable;
import me.megamichiel.animationlib.config.ConfigSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static java.util.Locale.ENGLISH;

public class CommandExecutor extends AbsAnimatable<List<BiPredicate<AnimatedMenuPlugin, Player>>> implements Consumer<Player> {

    private final AnimatedMenuPlugin plugin;
    
    public CommandExecutor(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    private <T, C> BiPredicate<AnimatedMenuPlugin, Player> getCommandHandler(Command<T, C> command,
                                                                             AnimatedMenuPlugin plugin, String str) {
        T val = command.parse(plugin, str);
        if (val == null) {
            return null;
        }
        C cached = command.tryCacheValue(plugin, val);
        return cached != null ?
                (plug, p) -> command.executeCached(plug, p, cached) :
                (plug, p) -> command.execute(plug, p, val);
    }

    @Override
    protected List<BiPredicate<AnimatedMenuPlugin, Player>> get(Nagger nagger, Object o) {
        List<BiPredicate<AnimatedMenuPlugin, Player>> result = new ArrayList<>();
        if (!(o instanceof List)) {
            return result;
        }
        for (Object raw : (List) o) {
            if (raw instanceof ConfigSection) {
                ((ConfigSection) raw).forEach((key, value) -> {
                    Command<?, ?> command = plugin.findCommand(key.trim().toLowerCase(ENGLISH));
                    if (command == TextCommand.DEFAULT) {
                        return;
                    }
                    if (value instanceof List<?>) {
                        BiPredicate<AnimatedMenuPlugin, Player> handler;
                        for (Object ele : ((List) value)) {
                            if (!(ele instanceof List<?> || ele instanceof ConfigSection) && (handler = getCommandHandler(command, plugin, ele.toString())) != null) {
                                result.add(handler);
                            }
                        }
                    } else if (!(value instanceof ConfigSection)) {
                        BiPredicate<AnimatedMenuPlugin, Player> handler = getCommandHandler(command, plugin, value.toString());
                        if (handler != null) {
                            result.add(handler);
                        }
                    }
                });
            } else if (!(raw instanceof Collection<?>)) {
                String str = raw.toString();
                int index = str.indexOf(':');
                Command<?, ?> cmd;
                if (index <= 0) {
                    cmd = TextCommand.DEFAULT;
                } else if ((cmd = plugin.findCommand(str.substring(0, index).trim())) == TextCommand.DEFAULT) {
                    nagger.nag("No command with id '" + str.substring(0, index) + "' found! Defaulting to player command");
                } else {
                    str = str.substring(index + 1).trim();
                }
                BiPredicate<AnimatedMenuPlugin, Player> handler = getCommandHandler(cmd, plugin, str);
                if (handler != null) {
                    result.add(handler);
                }
            }
        }
        return result;
    }

    @Override
    public void accept(Player player) {
        tick();
        List<BiPredicate<AnimatedMenuPlugin, Player>> commands = get();
        if (commands != null) {
            for (BiPredicate<AnimatedMenuPlugin, Player> predicate : commands) {
                if (!predicate.test(plugin, player)) {
                    break;
                }
            }
        }
    }
}
