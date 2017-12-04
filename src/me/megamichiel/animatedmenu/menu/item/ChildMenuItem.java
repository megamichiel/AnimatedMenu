package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.menu.MenuSession;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class ChildMenuItem implements IMenuItem {

    private final IMenuItem parent;

    public ChildMenuItem(IMenuItem parent) {
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        this.parent = parent;
    }

    @Override
    public int tick() {
        return parent.tick();
    }

    @Override
    public int getSlot(Player player, MenuSession session) {
        return parent.getSlot(player, session);
    }
    @Override
    public double getWeight(Player player, MenuSession session) {
        return parent.getWeight(player, session);
    }

    @Override
    public ItemStack getItem(Player player, MenuSession session, ItemStack item) {
        return parent.getItem(player, session, item);
    }

    @Override
    public void click(Player player, MenuSession session, ClickType type) {
        parent.click(player, session, type);
    }
}
