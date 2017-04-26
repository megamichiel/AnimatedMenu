package me.megamichiel.animatedmenu.menu;

import com.google.common.collect.BiMap;
import me.megamichiel.animatedmenu.menu.item.ItemInfo;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public final class MenuItem {

    private final ItemInfo info;
    private final int id, frameDelay, refreshDelay;
    private int frameTick, refreshTick;

    MenuItem(ItemInfo info, int id) {
        this.info = info;
        this.id = id;
        frameDelay = frameTick = Math.abs(info.getDelay(false));
        refreshDelay = refreshTick = Math.abs(info.getDelay(true));
    }

    boolean tick() {
        if (frameTick-- == 0) {
            frameTick = frameDelay;
            info.nextFrame();
        }
        return refreshTick-- == 0;
    }

    void postTick() {
        if (refreshTick == -1)
            refreshTick = refreshDelay;
    }

    boolean canRefresh() {
        return refreshTick == -1;
    }

    public void setFrameTick(int i) {
        frameTick = Math.max(i, 0);
    }

    public void setRefreshTick(int i) {
        refreshTick = Math.max(i, 0);
    }

    public void requestRefresh() {
        refreshTick = 0;
    }

    public ItemInfo getInfo() {
        return info;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof MenuItem && ((MenuItem) obj).info == info);
    }

    public static class SlotContext implements Nagger {

        private static int[] AMOUNT_DEFAULTS = new int[54];
        private static double[] WEIGHT_DEFAULTS = new double[54];

        static {
            Arrays.fill(AMOUNT_DEFAULTS, Integer.MIN_VALUE);
        }

        private final Nagger nagger;
        private final int[] amounts;
        private final double[] weights;

        private BiMap<Integer, MenuItem> items;
        private ItemStack stack;
        private double weight;

        SlotContext(Nagger nagger, int length, BiMap<Integer, MenuItem> items) {
            this.nagger = nagger;
            amounts = new int[length];
            weights = new double[length];

            if (length > AMOUNT_DEFAULTS.length) {
                AMOUNT_DEFAULTS = new int[length];
                Arrays.fill(AMOUNT_DEFAULTS, Integer.MIN_VALUE);
                WEIGHT_DEFAULTS = new double[length];
            }
            System.arraycopy(AMOUNT_DEFAULTS, 0, amounts, 0, amounts.length);

            this.items = items;
        }

        public void shiftRight(int slot) {
            if (!items.containsKey(slot)) return;

            int end = slot, len = weights.length;
            while (++end < len) {
                if (!items.containsKey(end)) break;
            }
            if (end == len) --end; // Don't want no index out of bounds stuff

            /* Shift array elements right */
            System.arraycopy(amounts, slot, amounts, slot + 1, end - slot);
            System.arraycopy(weights, slot, weights, slot + 1, end - slot);
            weights[slot] = 0D;
            amounts[slot] = 0;

            /* Update items map */
            while (end > slot) {
                MenuItem left = items.get(end - 1);
                if (left != null) items.forcePut(end, left);
                else break;
                --end;
            }
            items.remove(slot);
        }

        public BiMap<Integer, MenuItem> getItems() {
            return items;
        }

        public MenuItem getItem(int slot) {
            return items.get(slot);
        }

        public int[] getAmounts() {
            return amounts;
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

        int getSlot(Player player, MenuSession session, ItemInfo item, ItemStack stack) {
            this.stack = stack;
            weight = item.getWeight(player, session, this);
            int slot = item.getSlot(player, session, this);
            if (slot < 0 || slot >= amounts.length) return -1;
            amounts[slot] = stack.getAmount();
            weights[slot] = weight;
            return slot;
        }

        void addItem(Player player, MenuSession session, int slot, ItemInfo info, int amount) {
            amounts[slot] = amount;
            weights[slot] = info.getWeight(player, session, this);
        }

        void setItems(BiMap<Integer, MenuItem> items) {
            this.items = items;
        }

        void reset() {
            System.arraycopy(AMOUNT_DEFAULTS, 0, amounts, 0, amounts.length);
            System.arraycopy(WEIGHT_DEFAULTS, 0, weights, 0, weights.length);
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
