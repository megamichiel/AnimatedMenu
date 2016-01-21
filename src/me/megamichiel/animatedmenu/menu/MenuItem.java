package me.megamichiel.animatedmenu.menu;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@ToString
public class MenuItem {
	
	@Getter @Setter
	private ItemStack handle;
	@Getter
	private final MenuItemSettings settings;
	private final AtomicInteger currentTick = new AtomicInteger();
	
	public MenuItem(MenuItemSettings settings) {
		this.settings = settings;
		handle = load(settings);
	}
	
	public void tick() {
		if(currentTick.getAndIncrement() == settings.getFrameDelay()) {
			currentTick.set(0);
			settings.apply(handle);
		}
	}
	
	private static ItemStack load(MenuItemSettings settings) {
		ItemStack item = settings.getMaterial().next().clone();
		settings.applyFirst(item);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(settings.getDisplayName().next().toString());
		meta.setLore(settings.getLore().next().toStringList());
		item.setItemMeta(meta);
		return item;
	}
}
