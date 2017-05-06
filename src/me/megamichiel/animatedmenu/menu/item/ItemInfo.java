package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.menu.MenuItem;
import me.megamichiel.animatedmenu.menu.MenuSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface ItemInfo {

    int getDelay(DelayType type);
    default void nextFrame() {}

    default boolean hasFixedSlot() {
        return false;
    }
    int getSlot(Player player, MenuSession session, MenuItem.SlotContext ctx);
    default double getWeight(Player player, MenuSession session, MenuItem.SlotContext ctx) {
        return 0;
    }

    default ItemStack load(Player player, MenuSession session) {
        return apply(player, session, new ItemStack(Material.AIR));
    }
    ItemStack apply(Player player, MenuSession session, ItemStack item);

    void click(Player player, MenuSession session, ClickType type);

    enum DelayType {
        FRAME, REFRESH, SLOT
    }
}
