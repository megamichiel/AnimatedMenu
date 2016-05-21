package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.event.inventory.InventoryType;

public class MenuType {
    
    public static final MenuType HOPPER = new MenuType(InventoryType.HOPPER, "minecraft:hopper", 5, 1);
    public static final MenuType DISPENSER = new MenuType(InventoryType.DISPENSER, "minecraft:dispenser", 3, 3);
    public static final MenuType DROPPER = new MenuType(InventoryType.DROPPER, "minecraft:dropper", 3, 3);
    public static final MenuType CRAFTING = new MenuType(InventoryType.CRAFTING, "minecraft:crafting_table", 3, 3);
    
    public static final MenuType[] VALUES = new MenuType[] { HOPPER, DISPENSER, DROPPER, CRAFTING };
    
    private final InventoryType inventoryType;
    private final String nmsName;
    private final int width, height;
    
    public MenuType(InventoryType type, String nmsName, int width, int height) {
        inventoryType = type;
        this.nmsName = nmsName;
        this.width = width;
        this.height = height;
    }
    
    private MenuType(int rows) {
        inventoryType = InventoryType.CHEST;
        this.nmsName = "minecraft:chest";
        this.width = 9;
        this.height = rows;
    }

    public int getSize() {
        return width * height;
    }
    
    public AnimatedMenu newMenu(AnimatedMenuPlugin plugin, String name, AnimatedText title, int titleUpdateDelay,
                                StringBundle permission, StringBundle permissionMessage) {
        return new AnimatedMenu(plugin, name, title, titleUpdateDelay, this, permission, permissionMessage);
    }
    
    public static MenuType chest(int rows) {
        return new MenuType(rows);
    }
    
    public static MenuType fromName(String name) {
        for (MenuType type : VALUES)
            if (type.getInventoryType().name().equals(name))
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
