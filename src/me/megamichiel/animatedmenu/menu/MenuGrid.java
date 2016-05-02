package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;

public class MenuGrid {

	private final Supplier<AnimatedMenuPlugin> ref;
	private final MenuItem[] items;
	private int size = 0;
	
	public MenuGrid(Supplier<AnimatedMenuPlugin> ref, int size) {
		this.ref = ref;
        items = new MenuItem[size];
	}
	
	public void click(Player p, ClickType click, int slot) {
		MenuItem item = getItemAtInventorySlot(p, slot);
		if(item != null && item.getSettings().getClickListener() != null
				&& !item.getSettings().isHidden(ref.get(), p))
			item.getSettings().getClickListener().onClick(p, click);
	}
	
	public MenuItem getItemAtInventorySlot(Player p, int slot) {
		MenuItem item;
		for (int i = 0; i < items.length && (item = items[i]) != null; i++)
			if (item.getLastSlot(p) == slot)
				return item;
		return null;
	}
	
	public void addItem(MenuItem item) {
		items[size++] = item;
	}

	public void clear() {
		Arrays.fill(items, null);
	}

	public MenuItem[] getItems() {
		return items;
	}

	public int getSize() {
		return size;
	}
}
