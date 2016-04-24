package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.ReferenceHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;

public class MenuGrid {

	private final ReferenceHolder<AnimatedMenuPlugin> ref;
	private final MenuItem[] items;
	
	public MenuGrid(ReferenceHolder<AnimatedMenuPlugin> ref, int size) {
		this.ref = ref;
        items = new MenuItem[size];
	}
	
	public void click(Player p, ClickType click, int index) {
		MenuItem item = items[index];
		if(item != null && item.getSettings().getClickListener() != null
				&& !item.getSettings().isHidden(ref.get(), p))
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

	public void clear() {
		Arrays.fill(items, null);
	}

    public MenuItem[] getItems() {
        return items;
    }
}
