package me.megamichiel.animatedmenu.menu;

import lombok.Getter;

import org.bukkit.event.inventory.InventoryType;

public class MenuType {
	
	public static final MenuType HOPPER = new MenuType(InventoryType.HOPPER, 5);
	public static final MenuType DISPENSER = new MenuType(InventoryType.DISPENSER, 9);
	public static final MenuType DROPPER = new MenuType(InventoryType.DROPPER, 9);
	public static final MenuType CRAFTING = new MenuType(InventoryType.CRAFTING, 10);
	
	public static final MenuType[] VALUES = new MenuType[] {HOPPER, DISPENSER, DROPPER, CRAFTING};
	
	@Getter
	private final InventoryType inventoryType;
	@Getter
	private final int size;
	
	public MenuType(InventoryType type, int size) {
		inventoryType = type;
		this.size = size;
	}
	
	private MenuType(int rows) {
		inventoryType = InventoryType.CHEST;
		this.size = rows * 9;
	}
	
	public AnimatedMenu newMenu(String name, String title) {
		return new AnimatedMenu(name, title, this);
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
