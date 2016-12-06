package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface MenuItemInfo {

    int getDelay(boolean refresh);

    void nextFrame();

    boolean hasDynamicSlot();

    int getSlot(Player p, ItemStack[] contents, ItemStack stack);

    default ItemStack load(Nagger nagger, Player player) {
        return apply(nagger, player, new ItemStack(Material.AIR));
    }

    ItemStack apply(Nagger nagger, Player player, ItemStack item);

    boolean isHidden(AnimatedMenuPlugin plugin, Player player);

    void click(Player player, ClickType type);
}
