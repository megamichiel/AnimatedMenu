package me.megamichiel.animatedmenu.menu;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuItem {
	
	@Getter
	private final MenuItemSettings settings;
	private final AtomicInteger currentTick = new AtomicInteger();
	
	public MenuItem(MenuItemSettings settings) {
		this.settings = settings;
	}
	
	public boolean tick() {
		if(currentTick.getAndIncrement() == settings.getFrameDelay())
		{
			currentTick.set(0);
			settings.next();
			return true;
		}
		return false;
	}
	
	public void apply(Player p, ItemStack handle)
	{
		settings.apply(p, handle);
	}
	
	public ItemStack load(Player p) {
		ItemStack item = settings.getMaterial().get().clone();
		item = settings.applyFirst(item);
		
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(settings.getDisplayName().get().toString(p));
		meta.setLore(settings.getLore().get().toStringList(p));
		item.setItemMeta(meta);
		return item;
	}
}
