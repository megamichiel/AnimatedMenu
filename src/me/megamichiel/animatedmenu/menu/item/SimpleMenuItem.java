package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.menu.MenuSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleMenuItem extends TimedMenuItem {

    public static ItemStack createItem(Material type, int amount, short durability) {
        return createItem(type, amount, durability, null);
    }

    public static ItemStack createItem(Material type, int amount, short durability, Consumer<? super ItemMeta> meta) {
        ItemStack item = new ItemStack(type, amount, durability);
        ItemMeta im = item.getItemMeta();
        if (meta != null) {
            meta.accept(im);
        }
        item.setItemMeta(im);
        return item;
    }

    private final int slot;
    private final Function<Player, ItemStack> item;
    private final ClickListener listener;

    public SimpleMenuItem(int slot, ItemStack stack, ClickListener listener) {
        this.slot = slot;
        this.item = player -> stack;
        this.listener = listener;
    }

    public SimpleMenuItem(int slot, Function<Player, ItemStack> item, ClickListener listener) {
        this.slot = slot;
        this.item = item;
        this.listener = listener;
    }

    @Override
    public int getSlot(Player player, MenuSession session) {
        return slot;
    }

    @Override
    public ItemStack getItem(Player player, MenuSession session, ItemStack item, int tick) {
        return item == null ? this.item.apply(player) : item;
    }

    @Override
    public void click(Player player, MenuSession session, ClickType type) {
        if (listener != null) listener.onClick(player, type);
    }

    public interface ClickListener {
        void onClick(Player player, ClickType type);
    }
}
