package me.megamichiel.animatedmenu.animation;

import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animationlib.util.ArrayUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.bukkit.util.NumberConversions.floor;

public class OpenAnimation {

    private final byte[][] frames;
    private final double speed;

    private double frame = 0;

    private OpenAnimation(byte[][] frames, double speed) {
        this.frames = frames;
        this.speed = speed;
    }

    OpenAnimation(Type type, AbstractMenu.Type menuType, double speed) {
        this(type.sort(menuType), speed);
    }

    public OpenAnimation copy() {
        return new OpenAnimation(frames, speed);
    }

    public boolean tick(IntConsumer action) {
        int i = floor(frame), to = min(floor(frame += speed), frames.length);
        while (i < to) {
            for (int j : frames[i++]) {
                if (j >= 0) {
                    action.accept(j);
                }
            }
        }
        return i >= frames.length;
    }

    public interface Type {

        byte[][] sort(AbstractMenu.Type type);

        default OpenAnimation newAnimation(AbstractMenu.Type type) {
            return new OpenAnimation(this, type, 1.0);
        }

        default OpenAnimation newAnimation(AbstractMenu.Type type, double speed) {
            return new OpenAnimation(this, type, speed);
        }
    }

    public enum DefaultType implements Type {

        DOWN(type -> {
            int width = type.getWidth(), height = type.getHeight();
            byte[][] out = new byte[height][];
            for (int y = 0, slot = 0; y < height; ++y, slot += width) {
                byte[] slots = out[y] = new byte[width];
                for (int x = 0; x < width; ++x) {
                    slots[x] = (byte) (slot + x);
                }
            }
            return out;
        }), UP(DOWN),
        RIGHT(type -> {
            int width = type.getWidth(), height = type.getHeight();
            byte[][] out = new byte[width][];
            for (int x = 0; x < width; ++x) {
                byte[] slots = out[x] = new byte[height];
                for (int y = 0; y < height; ++y) {
                    slots[y] = (byte) (y * width + x);
                }
            }
            return out;
        }), LEFT(RIGHT),
        DOWN_RIGHT(type -> {
            int width = type.getWidth(), height = type.getHeight(), min = min(width, height);
            int count = width + height - 1;
            byte[][] out = new byte[count][];
            for (int i = 0; i < count; i++) {
                byte[] slots = new byte[min];
                int len = 0;
                for (int y = min(i, height - 1), x = max(i - y, 0); y >= 0 && x < width; ) {
                    slots[len++] = (byte) (y-- * width + x++);
                }
                System.arraycopy(slots, 0, out[i] = new byte[len], 0, len);
            }
            return out;
        }), UP_LEFT(DOWN_RIGHT),
        UP_RIGHT(type -> {
            int width = type.getWidth(), height = type.getHeight(), min = min(width, height), count = width + height - 1;
            byte[][] out = new byte[count][];
            for (int i = 0; i < count; i++) {
                byte[] slots = new byte[min];
                int len = 0;
                for (int y = max(height - 1 - i, 0), x = max(i - height + 1, 0); y < height && x < width; ) {
                    slots[len++] = (byte) (y++ * width + x++);
                }
                System.arraycopy(slots, 0, out[i] = new byte[len], 0, len);
            }
            return out;
        }), DOWN_LEFT(UP_RIGHT),
        OUT(type -> {
            int width = type.getWidth(), height = type.getHeight(),
                   x0 =  width - 1 >>> 1, x1 =  (width & 0x1) == 0 ? x0 + 1 : x0,
                   y0 = height - 1 >>> 1, y1 = (height & 0x1) == 0 ? y0 + 1 : y0;

            byte[][] out = new byte[max(x0, y0) + 1][];
            for (int pos = 0; x0 >= 0 || y0 >= 0; --x0, ++x1, --y0, ++y1) {
                byte[] slots = out[pos++] = new byte[x0 == x1 && y0 == y1 ? 1 : (x0 < 0 ? width : y0 < 0 ? height : x1 - x0 + y1 - y0) << 1];

                for (int index = 0, x = x0 < 0 ? 0 : x0; x <= x1 && x < width; ++x) {
                    for (int y = y0 < 0 ? 0 : y0; y <= y1 && y < height; ++y) {
                        if (x == x0 || x == x1 || y == y0 || y == y1) {
                            slots[index++] = (byte) (y * width + x);
                        }
                    }
                }
            }

            return out;
        }), IN(OUT),
        SNAKE_DOWN(type -> {
            int width = type.getWidth(), size = type.getSize();
            byte[][] out = new byte[size][];
            for (int i = 0, slot = 0; slot < size; slot += width) {
                if ((slot & 0x1) == 0) {
                    for (int x = 0; x < width; ) {
                        out[i++] = new byte[] { (byte) (slot + x++) };
                    }
                } else {
                    for (int x = width; x > 0; ) {
                        out[i++] = new byte[] { (byte) (slot + --x) };
                    }
                }
            }
            return out;
        }), SNAKE_UP(SNAKE_DOWN),
        SNAKE_RIGHT(type -> {
            int width = type.getWidth(), size = type.getSize();
            byte[][] out = new byte[size][];
            for (int i = 0, x = 0, slot = 0; x < width; ++x) {
                if ((x & 0x1) == 0) {
                    for (; slot < size; slot += width) {
                        out[i++] = new byte[] { (byte) (slot + x) };
                    }
                } else {
                    while (slot > 0) {
                        out[i++] = new byte[] { (byte) ((slot -= width) + x) };
                    }
                }
            }

            return out;
        }), SNAKE_LEFT(SNAKE_RIGHT);

        private final Function<AbstractMenu.Type, byte[][]> sorter;
        private final Map<AbstractMenu.Type, byte[][]> sorted = new HashMap<>();

        DefaultType(Function<AbstractMenu.Type, byte[][]> sorter) {
            this.sorter = sorter;
        }

        DefaultType(DefaultType opposite) {
            sorter = type -> {
                byte[][] sorted = opposite.sort(type);
                return sorted == null ? null : ArrayUtils.flip(sorted);
            };
        }

        @Override
        public byte[][] sort(AbstractMenu.Type type) {
            switch (type.getInventoryType()) {
                case CHEST:case HOPPER:case DROPPER:case DISPENSER:
                    return sorted.computeIfAbsent(type, sorter);
                default: return null;
            }
        }
    }
}
