package me.megamichiel.animatedmenu.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface MenuItemInfo {

    int getDelay(boolean refresh);

    void nextFrame();

    boolean hasDynamicSlot();

    int getSlot(Player p, SlotContext ctx);

    default ItemStack load(Player player) {
        return apply(player, new ItemStack(Material.AIR));
    }

    ItemStack apply(Player player, ItemStack item);

    boolean isHidden(Player player);

    void click(Player player, ClickType type);

    class SlotContext {

        private final ItemStack[] contents;
        private final int[] weights;
        private int[] slots, slotToPos;
        private MenuItem item;
        private ItemStack stack;

        SlotContext(ItemStack[] contents) {
            this.contents = contents;
            weights = new int[contents.length];
        }

        public void shiftRight(int slot) {
            int end = slot, len = contents.length;
            while (++end < len)
                if (contents[end] == null) break;
            if (end == len) end--;
            while (end > slot) {
                contents[end] = contents[end - 1];
                weights[end] = weights[end - 1];
                int i = slotToPos[end] = slotToPos[end - 1];
                if (i != -1) slots[i] = end;

                --end;
            }
        }

        public ItemStack[] getContents() {
            return contents;
        }

        public int[] getWeights() {
            return weights;
        }

        public MenuItem getItem() {
            return item;
        }

        public ItemStack getStack() {
            return stack;
        }

        public void setItem(MenuItem item, ItemStack stack) {
            this.item = item;
            this.stack = stack;
        }

        public void setSlots(int[] slots, int[] slotToPos) {
            this.slots = slots;
            this.slotToPos = slotToPos;
        }
    }
}
