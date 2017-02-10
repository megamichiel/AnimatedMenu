package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.menu.ItemInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class SimpleItemInfo implements ItemInfo {

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
        return 1000;
    }

    @Override
    public void nextFrame() {}

    @Override
    public boolean hasDynamicSlot() {
        return false;
    }

    @Override
    public int getSlot(Player player, SlotContext ctx) {
        return slot;
    }

    @Override
    public ItemStack load(Player player) {
        return stack;
    }

    @Override
    public ItemStack apply(Player player, ItemStack item) {
        return item;
    }

    @Override
    public void click(Player player, ClickType type) {
        if (listener != null) listener.onClick(player, type);
    }

    public interface ClickListener {
        void onClick(Player player, ClickType type);
    }
}
