package me.megamichiel.animatedmenu.menu.item;

import java.util.Arrays;

public abstract class TimedMenuItem implements IMenuItem {

    public static final int UPDATE_FRAME = IMenuItem.UPDATE_WEIGHT << 1;

    private final int[] values = new int[8];

    public TimedMenuItem() {
        Arrays.fill(values, -1);
    }

    public TimedMenuItem(int frameDelay, int refreshDelay, int slotDelay, int weightDelay) {
        set(frameDelay, refreshDelay, slotDelay, weightDelay);
    }

    protected void set(int frameDelay, int refreshDelay, int slotDelay, int weightDelay) {
        int[] values = this.values;
        values[0] = values[1] = Math.max(refreshDelay, -1);
        values[2] = values[3] = Math.max(slotDelay, -1);
        values[4] = values[5] = Math.max(weightDelay, -1);
        values[6] = values[7] = Math.max(frameDelay, -1);
    }

    protected void nextFrame() { }

    @Override
    public int tick() {
        int[] values = this.values;
        if (values[6] >= 0 && values[6]-- == 0) {
            nextFrame();
            values[6] = values[7];
        }
        int result = 0;
        for (int index = 0; (1 << index) <= UPDATE_WEIGHT; ++index) {
            if (values[index << 1] >= 0 && values[index << 1]-- == 0) {
                values[index << 1] = values[(index << 1) + 1];
                result |= 1 << index;
            }
        }
        return result;
    }

    public void refresh(int value) {
        for (int index = 0; (1 << index) <= UPDATE_FRAME; ++index) {
            if ((value & (1 << index)) != 0) {
                values[index << 1] = 0;
            }
        }
    }
}
