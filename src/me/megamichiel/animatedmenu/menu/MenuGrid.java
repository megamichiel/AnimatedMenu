package me.megamichiel.animatedmenu.menu;

import lombok.Getter;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class MenuGrid {
	
	@Getter
	private final MenuItem[] items;
	
	public MenuGrid(int size) {
		items = new MenuItem[size];
	}
	
	public void clear()
	{
		for (int i = 0; i < items.length; i++)
			items[i] = null;
	}
	
	public void click(Player p, ClickType click, int index) {
		MenuItem item = items[index];
		if(item != null && item.getSettings().getClickListener() != null
				&& !item.getSettings().isHidden(p))
			item.getSettings().getClickListener().onClick(p, click, items[index]);
	}
	
	public MenuItem getItem(int index) {
		return items[index];
	}
	
	public MenuItem setItem(int index, MenuItem item) {
		MenuItem old = items[index];
		items[index] = item;
		return old;
	}
}
