package me.megamichiel.animatedmenu.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public interface ItemClickListener {
	
	void onClick(Player who, ClickType click, MenuItem item);
	
}
