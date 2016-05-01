package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;

public abstract class Command<T, C> {

	final String prefix;

	protected Command(String prefix) {
		this.prefix = prefix;
	}

	protected abstract T parse(Nagger nagger, String command);

	protected abstract boolean execute(AnimatedMenuPlugin plugin, Player p, T value);

	protected abstract C tryCacheValue(AnimatedMenuPlugin plugin, T value);

	protected abstract boolean executeCached(AnimatedMenuPlugin plugin, Player p, C value);
}
