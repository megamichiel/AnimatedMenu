package me.megamichiel.animatedmenu.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface MenuItemInfo {

    int getDelay(boolean refresh);

    void nextFrame();

    boolean hasDynamicSlot();

    int getSlot(Player p, ItemStack[] contents, ItemStack stack);

    default ItemStack load(Player player) {
        return apply(player, new ItemStack(Material.AIR));
    }

    ItemStack apply(Player player, ItemStack item);

    boolean isHidden(Player player);

    void click(Player player, ClickType type);
}
