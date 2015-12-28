package me.megamichiel.animatedmenu;

import lombok.Getter;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AnimatedMenuPostLoadEvent extends Event {
	
	@Getter
	private static final HandlerList handlerList = new HandlerList();
	@Getter
	private final AnimatedMenuPlugin plugin;
	
	public AnimatedMenuPostLoadEvent(AnimatedMenuPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}
}
