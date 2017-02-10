package me.megamichiel.animatedmenu.menu;

import com.google.common.collect.BiMap;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface ItemInfo {

    int getDelay(boolean refresh);
    void nextFrame();

    boolean hasDynamicSlot();
    int getSlot(Player player, SlotContext ctx);
    default double getWeight(Player player, SlotContext ctx) {
        return 0;
    }

    default ItemStack load(Player player) {
        return apply(player, new ItemStack(Material.AIR));
    }
    ItemStack apply(Player player, ItemStack item);

    void click(Player player, ClickType type);

    default void menuClosed(Player player) {}

    class SlotContext implements Nagger {

        private final Nagger nagger;
        private final ItemStack[] contents;
        private final double[] weights;

        private BiMap<MenuItem, Integer> items;
        private ItemStack stack;
        private double weight;

        SlotContext(Nagger nagger, ItemStack[] contents) {
            this.nagger = nagger;
            this.contents = contents;
            weights = new double[contents.length];
        }

        public void shiftRight(int slot) {
            if (contents[slot] == null) return;

            int end = slot, len = contents.length;
            while (++end < len) {
                if (contents[end] == null) break;
            }
            if (end == len) --end; // Don't want no index out of bounds stuff

            /* Shift array elements right */
            System.arraycopy(contents, slot, contents, slot + 1, end - slot);
            System.arraycopy(weights, slot, weights, slot + 1, end - slot);
            contents[slot] = null;
            weights[slot] = 0D;

            /* Update items map */
            BiMap<Integer, MenuItem> inverse = items.inverse();
            while (end > slot) {
                MenuItem left = inverse.get(end - 1);
                if (left != null) inverse.put(end, left);
                --end;
            }
            inverse.remove(slot);
        }

        public ItemStack[] getContents() {
            return contents;
        }

        public double[] getWeights() {
            return weights;
        }

        public ItemStack getStack() {
            return stack;
        }

        public double getWeight() {
            return weight;
        }

        int getSlot(Player player, ItemInfo item, ItemStack stack) {
            this.stack = stack;
            weight = item.getWeight(player, this);
            int slot = item.getSlot(player, this);
            weights[slot] = weight;
            return slot;
        }

        void setItems(BiMap<MenuItem, Integer> items) {
            this.items = items;
        }

        @Override
        public void nag(String s) {
            nagger.nag(s);
        }

        @Override
        public void nag(Throwable throwable) {
            nagger.nag(throwable);
        }
    }
}
