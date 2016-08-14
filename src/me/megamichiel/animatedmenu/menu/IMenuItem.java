package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface IMenuItem {

    boolean hasDynamicSlot();

    boolean tick();

    int getSlot(Player p, ItemStack[] contents,
                ItemStack stack, boolean updateSlots);

    void apply(Nagger nagger, Player p, ItemStack handle);

    ItemStack load(Nagger nagger, Player p);

    boolean isHidden(AnimatedMenuPlugin plugin, Player player);

    ItemStack getItem(Nagger nagger, Player player);

    void click(Player player, ClickType click);
}
