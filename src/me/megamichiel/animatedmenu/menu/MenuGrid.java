package me.megamichiel.animatedmenu.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class MenuGrid {

	private final AbstractMenu menu;
	private final MenuItem[] items;
	private int size = 0;
	
	MenuGrid(AbstractMenu menu, int size) {
		this.menu = menu;
        items = new MenuItem[size];
	}

	void sortSlots() {
		MenuItem[] solid = new MenuItem[size],
				dynamic = new MenuItem[size];
		int solidSize = 0, dynamicSize = 0;
		for (int i = 0; i < size; i++) {
			if (items[i].hasDynamicSlot()) dynamic[dynamicSize++] = items[i];
			else solid[solidSize++] = items[i];
		}
		System.arraycopy(solid, 0, items, 0, solidSize);
		System.arraycopy(dynamic, 0, items, solidSize, dynamicSize);
	}
	
	public void click(Player p, ClickType click, int slot) {
		MenuItem item = getItemAtInventorySlot(p, slot);
		if(item != null && item.getSettings().getClickListener() != null
				&& !item.getSettings().isHidden(menu.getPlugin(), p))
			item.getSettings().getClickListener().onClick(p, click);
	}
	
	private MenuItem getItemAtInventorySlot(Player p, int slot) {
		MenuItem item;
		for (int i = 0; i < size; i++)
			if ((item = items[i]).getLastSlot(p) == slot)
				return item;
		return null;
	}
	
	public void addItem(MenuItem item) {
		items[size++] = item;
	}

	public void clear() {
		while (size > 0)
			items[--size] = null;
	}

	public MenuItem[] getItems() {
		return items;
	}

	public int getSize() {
		return size;
	}
}
