package me.megamichiel.animatedmenu.animation;

import me.megamichiel.animatedmenu.menu.MenuType;
import org.bukkit.util.NumberConversions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static me.megamichiel.animationlib.util.ArrayUtils.flip;

public class OpenAnimation {

    private final int[][] frames;
    private final double speed;

    public OpenAnimation(int[][] frames, double speed) {
        this.frames = frames;
        this.speed = speed;
    }

    public Animation newAnimation(Consumer<int[]> action) {
        return new Animation(action);
    }

    public class Animation {

        private final Consumer<int[]> action;
        private double frame = 0;

        private Animation(Consumer<int[]> action) {
            this.action = action;
        }

        public boolean tick() {
            double last = frame;
            frame += speed;
            int i = NumberConversions.ceil(last);
            do {
                if (i == frames.length) return true;
                action.accept(frames[i]);
            } while (++i <= frame);
            return i == frames.length;
        }
    }

    public interface Type {
        int[][] sort(MenuType type);
    }

    public enum DefaultType implements Type {

        DOWN(type -> {
            int width = type.getWidth(), height = type.getHeight();
            int[][] out = new int[height][];
            for (int y = 0, slot = 0; y < height; y++, slot += width) {
                int[] slots = out[y] = new int[width];
                Arrays.fill(slots, -1);
                for (int x = 0; x < width; x++)
                    slots[x] = slot + x;
            }
            return out;
        }), UP(type -> flip(DOWN.sorter.apply(type))),
        RIGHT(type -> {
            int width = type.getWidth(), height = type.getHeight();
            int[][] out = new int[width][];
            for (int x = 0; x < width; x++) {
                int[] slots = out[x] = new int[height];
                Arrays.fill(slots, -1);
                for (int y = 0; y < height; y++)
                    slots[y] = y * width + x;
            }
            return out;
        }), LEFT(type -> flip(RIGHT.sorter.apply(type))),
        DOWN_RIGHT(type -> {
            int width = type.getWidth(), height = type.getHeight(),
                    min = Math.min(width, height);
            int count = width + height - 1;
            int[][] out = new int[count][];
            for (int i = 0; i < count; i++) {
                int[] slots = out[i] = new int[min];
                Arrays.fill(slots, -1);
                for (int x = 0, y = i, j = 0; y >= 0; x++, y--)
                    if (x < width && y < height) slots[j++] = y * width + x;
            }
            return out;
        }), UP_LEFT(type -> flip(DOWN_RIGHT.sorter.apply(type))),
        UP_RIGHT(type -> {
            int width = type.getWidth(), height = type.getHeight(),
                    min = Math.min(width, height);
            int count = width + height - 1;
            int[][] out = new int[count][];
            for (int i = 0; i < count; i++) {
                int[] slots = out[i] = new int[min];
                Arrays.fill(slots, -1);
                for (int x = 0, y = height - i - 1, j = 0; y < height; x++, y++)
                    if (x < width && y >= 0) slots[j++] = y * width + x;
            }
            return out;
        }), DOWN_LEFT(type -> flip(UP_RIGHT.sorter.apply(type))),
        OUT(type -> {
            int width = type.getWidth(), height = type.getHeight(),
                    midX = width / 2, midY = height / 2;
            int hor = ~width & 0x1, ver = ~height & 0x1;
            int count = Math.max(midX, midY) + 1;
            int[][] out = new int[count][];
            int[] slots;
            for (int i = 0; i < count; i++) {
                int slot = 0;
                if (i == 0) {
                    slots = out[i] = new int[(hor + 1) * (ver + 1)];
                    for (int x = -hor; x <= 0; x++)
                        for (int y = -ver; y <= 0; y++)
                            slots[slot++] = (midY + y) * width + midX + x;
                } else {
                    slots = out[i] = new int[8 * i + 2 * hor + 2 * ver];
                    Arrays.fill(slots, -1);
                    int minX = midX - i - hor, maxX = midX + i,
                            minY = midY - i - ver, maxY = midY + i;
                    for (int x = minX; x <= maxX; x++)
                        for (int y = minY; y <= maxY; y++)
                            if ((x == minX || x == maxX || y == minY || y == maxY)
                                    && x >= 0 && x < width && y >= 0 && y < height)
                                slots[slot++] = y * width + x;
                }
            }
            return out;
        }), IN(type -> flip(OUT.sorter.apply(type))),
        SNAKE_DOWN(type -> {
            int width = type.getWidth(), height = type.getHeight();
            int count = width * height;
            int[][] out = new int[count][];
            int i = 0;
            for (int y = 0; y < height; y++) {
                int slot = y * width;
                if ((y & 0x1) == 0) for (int x = 0; x < width; x++)
                    out[i++] = new int[] { slot + x };
                else for (int x = width; x-- != 0;)
                    out[i++] = new int[] { slot + x };
            }
            return out;
        }), SNAKE_UP(type -> flip(SNAKE_DOWN.sorter.apply(type))),
        SNAKE_RIGHT(type -> {
            int width = type.getWidth(), height = type.getHeight();
            int count = width * height;
            int[][] out = new int[count][];
            int i = 0;
            for (int x = 0; x < width; x++)
                if ((x & 0x1) == 0) for (int y = 0; y < height; y++)
                    out[i++] = new int[] { y * width + x };
                else for (int y = height; y-- != 0;)
                    out[i++] = new int[] { y * width + x };
            return out;
        }), SNAKE_LEFT(type -> flip(SNAKE_RIGHT.sorter.apply(type)));

        private final Function<MenuType, int[][]> sorter;
        private final Map<MenuType, int[][]> sorted = new HashMap<>();

        DefaultType(Function<MenuType, int[][]> sorter) {
            this.sorter = sorter;
        }

        @Override
        public int[][] sort(MenuType type) {
            switch (type.getInventoryType()) {
                case CHEST:case HOPPER:case DROPPER:case DISPENSER:
                    return sorted.computeIfAbsent(type, sorter);
                default: return null;
            }
        }
    }
}
