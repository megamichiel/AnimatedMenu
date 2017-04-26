package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.menu.MenuItem;
import me.megamichiel.animatedmenu.menu.MenuSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

public class SimpleItemInfo implements ItemInfo {

    public static ItemStack createItem(Material type, int amount, short durability) {
        return createItem(type, amount, durability, meta -> {});
    }

    public static ItemStack createItem(Material type, int amount, short durability, Consumer<? super ItemMeta> meta) {
        ItemStack item = new ItemStack(type, amount, durability);
        ItemMeta im = item.getItemMeta();
        meta.accept(im);
        item.setItemMeta(im);
        return item;
    }

    private final int slot;
    private final ItemStack stack;
    private final ClickListener listener;

    public SimpleItemInfo(int slot, ItemStack stack, ClickListener listener) {
        this.slot = slot;
        this.stack = stack;
        this.listener = listener;
    }

    @Override
    public int getDelay(boolean refresh) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void nextFrame() {}

    @Override
    public boolean hasFixedSlot() {
        return true;
    }

    @Override
    public int getSlot(Player player, MenuSession session, MenuItem.SlotContext ctx) {
        return slot;
    }

    @Override
    public ItemStack load(Player player, MenuSession session) {
        return stack;
    }

    @Override
    public ItemStack apply(Player player, MenuSession session, ItemStack item) {
        return item;
    }

    @Override
    public void click(Player player, MenuSession session, ClickType type) {
        if (listener != null) listener.onClick(player, type);
    }

    public interface ClickListener {
        void onClick(Player player, ClickType type);
    }
}
