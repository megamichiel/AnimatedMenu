package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;

public abstract class Command<T, C> {

	private final String prefix;

	protected Command(String prefix) {
		this.prefix = prefix;
	}

    public String getPrefix() {
        return prefix;
    }

	public abstract T parse(Nagger nagger, String command);
	
	public abstract boolean execute(AnimatedMenuPlugin plugin, Player p, T value);

	public abstract C tryCacheValue(Nagger nagger, T value);

    public abstract boolean executeCached(AnimatedMenuPlugin plugin, Player p, C value);
}
