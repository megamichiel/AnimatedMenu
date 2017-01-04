package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.event.inventory.InventoryType;

public class MenuType {
    
    public static final MenuType HOPPER = new MenuType(InventoryType.HOPPER, "minecraft:hopper", 5, 1);
    public static final MenuType DISPENSER = new MenuType(InventoryType.DISPENSER, "minecraft:dispenser", 3, 3);
    public static final MenuType DROPPER = new MenuType(InventoryType.DROPPER, "minecraft:dropper", 3, 3);
    public static final MenuType CRAFTING = new MenuType(InventoryType.CRAFTING, "minecraft:crafting_table", 3, 3);
    
    public static final MenuType[] VALUES = new MenuType[] { HOPPER, DISPENSER, DROPPER, CRAFTING };

    private static final MenuType[] CHEST = new MenuType[6];
    
    private final InventoryType inventoryType;
    private final String nmsName;
    private final int width, height, size;
    
    public MenuType(InventoryType type, String nmsName, int width, int height) {
        inventoryType = type;
        this.nmsName = nmsName;
        size = (this.width = width) * (this.height = height);
    }
    
    private MenuType(int rows) {
        this(InventoryType.CHEST, "minecraft:chest", 9, rows);
    }

    public int getSize() {
        return size;
    }
    
    public static MenuType chest(int rows) {
        MenuType type = CHEST[rows - 1];
        if (type == null) return CHEST[rows - 1] = new MenuType(rows);
        return type;
    }
    
    public static MenuType fromName(String name) {
        for (MenuType type : VALUES)
            if (type.inventoryType.name().equals(name))
                return type;
        return null;
    }

    public InventoryType getInventoryType() {
        return this.inventoryType;
    }

    public String getNmsName() {
        return this.nmsName;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
