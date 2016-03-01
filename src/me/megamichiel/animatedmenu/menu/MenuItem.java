package me.megamichiel.animatedmenu.menu;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuItem {
	
	@Getter
	private final MenuItemSettings settings;
	private final AtomicInteger frameTick = new AtomicInteger(), refreshTick = new AtomicInteger();
	
	public MenuItem(MenuItemSettings settings) {
		this.settings = settings;
	}
	
	public boolean tick() {
		if (frameTick.getAndIncrement() == settings.getFrameDelay())
		{
			frameTick.set(0);
			settings.next();
		}
		if (refreshTick.getAndIncrement() == settings.getRefreshDelay())
		{
			refreshTick.set(0);
			return true;
		}
		return false;
	}
	
	public void apply(Nagger nagger, Player p, ItemStack handle)
	{
		settings.apply(nagger, p, handle);
	}
	
	public ItemStack load(Nagger nagger, Player p) {
		ItemStack item = settings.getMaterial().get().toItemStack(nagger, p);
		item = settings.applyFirst(nagger, p, item);
		
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(settings.getDisplayName().get().toString(p));
		meta.setLore(settings.getLore().get().toStringList(p));
		item.setItemMeta(meta);
		return item;
	}
}
