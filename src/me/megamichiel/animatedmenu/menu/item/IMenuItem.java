package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.menu.MenuSession;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface IMenuItem {

    int UPDATE_SLOT = 1 << 29, UPDATE_WEIGHT = 1 << 30, UPDATE_ITEM = Integer.MAX_VALUE & ~(UPDATE_SLOT | UPDATE_WEIGHT), REMOVE = -1;
    int SLOT_SHIFT_RIGHT = 1 << 29, SLOT_SHIFT_LEFT = 1 << 30, NO_SLOT = Integer.MAX_VALUE & ~(SLOT_SHIFT_LEFT | SLOT_SHIFT_RIGHT);

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
