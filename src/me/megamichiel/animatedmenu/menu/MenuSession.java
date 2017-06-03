package me.megamichiel.animatedmenu.menu;

import com.google.common.collect.BiMap;
import me.megamichiel.animatedmenu.util.InventoryTitleUpdater;
import me.megamichiel.animationlib.util.ReflectClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class MenuSession {

    private static final InventoryTitleUpdater TITLE_UPDATER;

    static {
        InventoryTitleUpdater updater;
        try {
            updater = new InventoryTitleUpdater();
        } catch (ReflectClass.ReflectException ex) {
            updater = null;
        }
        TITLE_UPDATER = updater;
    }

    private final AbstractMenu menu;
    private final Player player;
    private final Inventory inventory;

    private final BiMap<MenuItem, Integer> items;
    private final Map<Property, Object> properties = new ConcurrentHashMap<>();
    private final Queue<Task> tasks = new ConcurrentLinkedQueue<>();

    private String title;
    private BooleanSupplier openAnimation;

    MenuSession(AbstractMenu menu, Player player, Inventory inventory, BiMap<MenuItem, Integer> items, BooleanSupplier openAnimation) {
        this.menu = menu;
        this.player = player;
        this.inventory = inventory;
        this.items = items;
        this.openAnimation = openAnimation;
        title = inventory.getTitle();
    }

    public void close() {
        player.closeInventory();
    }

    BiMap<MenuItem, Integer> getItems() {
        return items;
    }

    public Player getPlayer() {
        return player;
    }

    public void setTitle(String title) {
        if (title == null ? this.title != null : !title.equals(this.title)) {
            this.title = title;
            if (TITLE_UPDATER != null) {
                TITLE_UPDATER.update(player, inventory, title);
            }
        }
    }

    public AbstractMenu getMenu() {
        return menu;
    }

    Inventory getInventory() {
        return inventory;
    }

    public MenuItem getItem(int slot) {
        return items.inverse().get(slot);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Property<T> property) {
        return (T) properties.getOrDefault(property, property.defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Property<T> property, T defaultValue) {
        return (T) properties.getOrDefault(property, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T set(Property<T> property, T value) {
        Object old = value != null ? properties.put(property, value) : properties.remove(property);
        return old != null ? (T) old : property.defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T compute(Property<T> property, Supplier<T> supplier) {
        return (T) properties.computeIfAbsent(property, prop -> supplier.get());
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(Property<T> property) {
        Object old = properties.remove(property);
        return old != null ? (T) old : property.defaultValue;
    }

    public boolean isSet(Property<?> property) {
        return properties.containsKey(property);
    }

    public boolean isOpening() {
        return openAnimation != null;
    }

    public Task schedule(Runnable task, long delay) {
        return schedule(task, delay, -1L);
    }

    public Task schedule(Runnable task, long delay, long period) {
        if (task == null) throw new IllegalArgumentException("Task is null!");
        if (delay < 0L) throw new IllegalArgumentException("Delay must be >= 0!");
        if (period < -1L) throw new IllegalArgumentException("Period must be >= -1");

        Task t = new Task(task, delay, period);
        tasks.add(t);
        return t;
    }

    boolean tick() {
        tasks.removeIf(Task::tick);
        if (openAnimation != null) {
            if (openAnimation.getAsBoolean()) {
                return true;
            } else {
                openAnimation = null;
            }
        }
        return false;
    }

    public static class Property<T> {

        public static <T> Property<T> of() {
            return new Property<>(null);
        }

        public static <T> Property<T> of(T defaultValue) {
            return new Property<>(defaultValue);
        }

        private final T defaultValue;
        private final int hash = super.hashCode();

        private Property(T defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static class Task {

        private final Runnable task;
        private final long period;

        private boolean cancelled;
        private long tick;

        Task(Runnable task, long delay, long period) {
            this.task = task;
            this.period = period;
            tick = delay;
        }

        boolean tick() {
            if (cancelled) {
                return true;
            }
            if (tick-- == 0L) {
                task.run();
                if ((tick = period) == -1L) {
                    return true;
                }
            }
            return false;
        }

        public void cancel() {
            cancelled = true;
        }
    }
}
