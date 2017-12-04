package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;

import java.util.Locale;

public abstract class Command<Type, Cached> {

    private final String prefix;

    protected Command(String prefix) {
        this.prefix = prefix.toLowerCase(Locale.ENGLISH);
    }

    public final String getPrefix() {
        return prefix;
    }

    protected abstract Type parse(Nagger nagger, String command);

    protected abstract boolean execute(AnimatedMenuPlugin plugin, Player p, Type value);

    protected abstract Cached tryCacheValue(AnimatedMenuPlugin plugin, Type value);

    protected abstract boolean executeCached(AnimatedMenuPlugin plugin, Player p, Cached value);
}
