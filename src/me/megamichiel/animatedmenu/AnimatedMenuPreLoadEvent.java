package me.megamichiel.animatedmenu;

import lombok.Getter;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 
 * Called before menus are loaded. Add stuff like command handlers here
 *
 */
public class AnimatedMenuPreLoadEvent extends Event {
	
	@Getter
	private static final HandlerList handlerList = new HandlerList();
	@Getter
	private final AnimatedMenuPlugin plugin;
	
	public AnimatedMenuPreLoadEvent(AnimatedMenuPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}
}
