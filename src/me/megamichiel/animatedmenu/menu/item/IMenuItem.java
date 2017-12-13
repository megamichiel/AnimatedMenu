package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.menu.MenuSession;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface IMenuItem {

    int UPDATE_ITEM = 1, UPDATE_SLOT = 2, UPDATE_WEIGHT = 4;

    default int tick() {
        return 0;
    }

    int getSlot(Player player, MenuSession session);
    default double getWeight(Player player, MenuSession session) {
        return 0;
    }

    ItemStack getItem(Player player, MenuSession session, ItemStack item, int tick);

    default void click(Player player, MenuSession session, ClickType type) { }
}
