package me.megamichiel.animatedmenu;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 
 * Called after the plugin has loaded menus. Add your own menus here
 *
 */
public class AnimatedMenuPostLoadEvent extends Event {

	private static final HandlerList handlerList = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlerList;
	}

	private final AnimatedMenuPlugin plugin;
	
	public AnimatedMenuPostLoadEvent(AnimatedMenuPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}

	public AnimatedMenuPlugin getPlugin() {
		return plugin;
	}
}
