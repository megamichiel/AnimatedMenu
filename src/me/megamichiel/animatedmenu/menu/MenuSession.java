package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.menu.item.IMenuItem;
import me.megamichiel.animatedmenu.util.Delay;
import me.megamichiel.animatedmenu.util.InventoryTitleUpdater;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.util.ReflectClass;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class MenuSession implements InventoryHolder, Nagger {

    private static final InventoryTitleUpdater TITLE_UPDATER;

    public static final long ONCE = -1L;

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
    private final Nagger nagger;
    private final Player player;
    private final Inventory inventory;

    private final Map<Property, Object> properties = new ConcurrentHashMap<>();
    private final Queue<Task> tasks = new ConcurrentLinkedQueue<>();

    private String title;
    private BooleanSupplier openAnimation;

    /* Grid fields */

    final Map<IMenuItem, Integer> slots = new HashMap<>();
    final GridEntry[] entries;
    boolean updateInventory = false;

    private ItemStack stack;
    private double weight;

    MenuSession(AbstractMenu menu, Nagger nagger, Player player, Function<MenuSession, Inventory> inventory) {
        this.menu = menu;
        this.nagger = nagger;
        this.player = player;
        this.entries = new GridEntry[menu.getType().getSize()];

        this.inventory = inventory.apply(this);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setOpenAnimation(BooleanSupplier openAnimation) {
        this.openAnimation = openAnimation;
    }

    public Player getPlayer() {
        return player;
    }

    public void setTitle(String title) {
        if (title == null ? this.title != null : !title.equals(this.title)) {
            this.title = title;
            if (TITLE_UPDATER != null && player.getOpenInventory().getTopInventory() == inventory) {
                TITLE_UPDATER.update(player, inventory, menu.getType().getNmsName(), title);
            }
        }
    }

    public AbstractMenu getMenu() {
        return menu;
    }

    public IMenuItem getItem(int slot) {
        GridEntry entry;
        return slot >= 0 && slot < entries.length ? (entry = entries[slot]) == null ? null : entry.item : null;
    }

    public boolean isEmpty(int slot) {
        return entries[slot] == null;
    }

    public boolean click(int slot, ClickType type) {
        GridEntry entry;
        IMenuItem item;
        if (slot < 0 || slot >= entries.length || (item = (entry = entries[slot]) == null ? menu.getEmptyItem() : entry.item) == null) {
            return false;
        }

        Delay delay = menu.getClickDelay();
        if (delay == null || delay.test(player)) {
            item.click(player, this, type);
        }
        return true;
    }

    public <T> T get(Property<T> property) {
        return (T) properties.getOrDefault(property, property.defaultValue);
    }

    public <T> T get(Property<T> property, T defaultValue) {
        return (T) properties.getOrDefault(property, defaultValue);
    }

    public <T> T set(Property<T> property, T value) {
        Object old = value != null ? properties.put(property, value) : properties.remove(property);
        return old != null ? (T) old : property.defaultValue;
    }

    public <T> T compute(Property<T> property, Supplier<T> supplier) {
        return (T) properties.computeIfAbsent(property, prop -> supplier.get());
    }

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

    public Task schedule(Runnable task, long delay, long period) {
        if (task == null) {
            throw new IllegalArgumentException("Task is null!");
        }
        if (delay < 0L) {
            throw new IllegalArgumentException("Delay must be >= 0!");
        }
        if (period < ONCE) {
            throw new IllegalArgumentException("Period must be >= -1");
        }

        Task t = new Task(task, delay, period);
        tasks.add(t);
        return t;
    }

    boolean tick() {
        tasks.removeIf(Task::tick);
        if (openAnimation != null) {
            if (openAnimation.getAsBoolean()) {
                openAnimation = null;
            } else {
                return true;
            }
        }
        return false;
    }

    /* Grid methods */

    public int getItemCount() {
        return slots.size();
    }

    public int getSize() {
        return entries.length;
    }

    public ItemStack getItemStack(int slot) {
        GridEntry entry = entries[slot];
        return entry == null ? null : entry.stack;
    }

    public Double getItemWeight(int slot) {
        GridEntry entry = entries[slot];
        return entry == null ? null : entry.weight;
    }

    public ItemStack getItemStack() {
        return stack;
    }

    public double getItemWeight() {
        return weight;
    }

    public void shiftRight(int slot) {
        if (stack == null) {
            throw new IllegalStateException("Cannot call this method right now!");
        }
        GridEntry[] entries = this.entries;
        if (entries[slot] == null) {
            return;
        }

        int end = slot, len = entries.length;
        while (++end < len) {
            if (entries[slot] == null) {
                break;
            }
        }
        Map<IMenuItem, Integer> slots = this.slots;
        if (end == len) {
            slots.remove(entries[--end].item); // Decrease end to prevent out of bounds
        }

        boolean update = updateInventory;
        Inventory inv = update ? inventory : null;

        /* Shift items right */
        for (GridEntry left; end > slot; ) {
            if ((left = entries[end - 1]) == null) {
                break;
            }
            slots.put((entries[end] = left).item, end--);
            if (update) {
                inv.setItem(end, inv.getItem(end + 1));
            }
        }
        entries[slot] = null;
        if (update) {
            inv.setItem(slot, null);
        }
    }

    int getSlot(IMenuItem item, ItemStack stack, boolean updateWeight) {
        this.stack = stack;
        int slot;
        if (updateWeight) {
            try {
                weight = item.getWeight(player, MenuSession.this);
            } catch (Exception ex) {
                nagger.nag("Failed to update weight of " + item + " in menu " + menu.getName() + "!", ex);
                this.stack = null;
                return -1;
            }
        }
        try {
            slot = item.getSlot(player, MenuSession.this);
        } catch (Exception ex) {
            nagger.nag("Failed to update slot of " + item + " in menu " + menu.getName() + "!", ex);
            return -1;
        } finally {
            this.stack = null;
        }

        GridEntry[] entries = this.entries;
        if (slot < 0 || slot >= entries.length) {
            return -1;
        }

        Integer old = slots.put(item, slot);

        GridEntry entry;
        if (old == null) {
            entry = entries[slot] = new GridEntry(item);
        } else if (slot == old){
            entry = entries[slot];
        } else {
            entry = entries[slot] = entries[old];
            entries[old] = null;
        }
        entry.stack = stack;
        if (updateWeight) {
            entry.weight = weight;
        }

        return slot;
    }

    /* Nagger stuff */

    @Override
    public void nag(String s) {
        nagger.nag(s);
    }

    @Override
    public void nag(Throwable throwable) {
        nagger.nag("An error occurred", throwable);
    }

    public static class GridEntry {

        final IMenuItem item;
        ItemStack stack;
        double weight;

        private GridEntry(IMenuItem item) {
            this.item = item;
        }

        public IMenuItem getItem() {
            return item;
        }

        public int getAmount() {
            return stack.getAmount();
        }

        public double getWeight() {
            return weight;
        }
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
                return (tick = period) == ONCE;
            }
            return false;
        }

        public void cancel() {
            cancelled = true;
        }
    }
}
