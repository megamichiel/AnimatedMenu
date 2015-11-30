package me.megamichiel.animatedmenu.menu;

import lombok.Getter;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

public class MenuGrid {
	
	private final Inventory inv;
	@Getter
	private final MenuItem[] items;
	
	public MenuGrid(Inventory inv, int size) {
		this.inv = inv;
		items = new MenuItem[size];
	}
	
	public void click(Player p, ClickType click, int index) {
		if(items[index] != null && items[index].getSettings().getClickListener() != null)
			items[index].getSettings().getClickListener().onClick(p, click, items[index]);
	}
	
	public MenuItem getItem(int index) {
		return items[index];
	}
	
	public MenuItem setItem(int index, MenuItem item) {
		MenuItem old = items[index];
		items[index] = item;
		inv.setItem(index, item.getHandle());
		item.setHandle(inv.getItem(index)); //Now editing the ItemStack will go directly into the inventory
		return old;
	}
}
