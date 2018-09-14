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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class MenuSession implements InventoryHolder, Nagger {

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
    private final Nagger nagger;
    final Inventory inventory;

    private final Map<Property, Object> properties = new ConcurrentHashMap<>();

    private String title;
    BooleanSupplier openAnimation;

    /* Grid fields */

    final Map<IMenuItem, GridEntry> slots = new HashMap<>();
    final GridEntry[] entries;
    boolean updateInventory = false;

    GridEntry entry;

    ItemStack emptyStack;

    MenuSession(AbstractMenu menu, Nagger nagger, Function<MenuSession, Inventory> inventory) {
        this.menu = menu;
        this.nagger = nagger;
        this.entries = new GridEntry[menu.getType().getSize()];

        this.inventory = inventory.apply(this);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setTitle(Player player, String title) {
        if (title == null ? this.title != null : !title.equals(this.title)) {
            this.title = title;
            if (TITLE_UPDATER != null && player.getOpenInventory().getTopInventory() == inventory) {
                try {
                    TITLE_UPDATER.update(player, inventory, menu.getType().getNmsName(), title);
                } catch (ReflectClass.ReflectException ex) {
                    nagger.nag("Couldn't update menu title!");
                    nagger.nag(ex);
                }
            }
        }
    }

    public AbstractMenu getMenu() {
        return menu;
    }

    public IMenuItem getItem(int slot) {
        GridEntry entry;
        return slot < 0 || slot >= entries.length || (entry = entries[slot]) == null ? null : entry.item;
    }

    public boolean isEmpty(int slot) {
        return getItem(slot) == null;
    }

    public boolean click(Player player, int slot, ClickType type) {
        GridEntry entry; IMenuItem item;
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

    public GridEntry getEntry() {
        return entry;
    }

    public void shiftRight(IMenuItem item, int slot) {
        if (entry == null) {
            throw new IllegalStateException("Cannot call this method right now!");
        }
        GridEntry them;

        GridEntry[] entries = this.entries;
        if ((them = entries[slot]) == null || them.item == item) {
            return;
        }

        int end = slot, len = entries.length;
        while (++end < len) {
            if (entries[slot] == null) {
                break;
            }
        }
        Map<IMenuItem, GridEntry> slots = this.slots;
        if (end == len) {
            slots.remove(entries[--end].item); // Decrease end to prevent out of bounds
        }

        boolean update = updateInventory;
        Inventory inv = update ? inventory : null;

        /* Shift items right */
        for (GridEntry left; end > slot && (left = entries[end - 1]) != null; ) {
            (entries[end] = left).slot = end--;
            if (update) {
                inv.setItem(end, inv.getItem(end + 1));
            }
        }
        entries[slot] = null;
        if (update) {
            inv.setItem(slot, null);
        }
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
        int slot = IMenuItem.NO_SLOT;
        ItemStack stack;
        double weight;

        GridEntry(IMenuItem item) {
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
}
