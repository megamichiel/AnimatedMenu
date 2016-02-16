package me.megamichiel.animatedmenu.menu;

import lombok.Getter;
import me.megamichiel.animatedmenu.animation.AnimatedText;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.event.inventory.InventoryType;

@Getter public class MenuType {
	
	public static final MenuType HOPPER = new MenuType(InventoryType.HOPPER, "minecraft:hopper", 5);
	public static final MenuType DISPENSER = new MenuType(InventoryType.DISPENSER, "minecraft:dispenser", 9);
	public static final MenuType DROPPER = new MenuType(InventoryType.DROPPER, "minecraft:dropper", 9);
	public static final MenuType CRAFTING = new MenuType(InventoryType.CRAFTING, "minecraft:crafting_table", 10);
	
	public static final MenuType[] VALUES = new MenuType[] { HOPPER, DISPENSER, DROPPER, CRAFTING };
	
	private final InventoryType inventoryType;
	private final String nmsName;
	private final int size;
	
	public MenuType(InventoryType type, String nmsName, int size) {
		inventoryType = type;
		this.nmsName = nmsName;
		this.size = size;
	}
	
	private MenuType(int rows) {
		inventoryType = InventoryType.CHEST;
		this.nmsName = "minecraft:chest";
		this.size = rows * 9;
	}
	
	public AnimatedMenu newMenu(Nagger nagger, String name, AnimatedText title, int titleUpdateDelay) {
		return new AnimatedMenu(nagger, name, title, titleUpdateDelay, this);
	}
	
	public static MenuType chest(int rows) {
		return new MenuType(rows);
	}
	
	public static MenuType fromName(String name) {
		for(MenuType type : VALUES)
			if(type.getInventoryType().name().equals(name))
				return type;
		return null;
	}
}
