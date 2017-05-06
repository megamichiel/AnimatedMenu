package me.megamichiel.animatedmenu.menu;

import com.google.common.collect.BiMap;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.OpenAnimation;
import me.megamichiel.animatedmenu.util.InventoryTitleUpdater;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MenuSession {

    private static final InventoryTitleUpdater TITLE_UPDATER = new InventoryTitleUpdater();

    private final AnimatedMenuPlugin plugin;
    private final AbstractMenu menu;
    private final Player player;

    final Inventory inventory;
    final BiMap<MenuItem, Integer> items;

    private String title;

    OpenAnimation.Animation opening;

    private final Map<Property, Object> properties = new ConcurrentHashMap<>();

    MenuSession(AnimatedMenuPlugin plugin, AbstractMenu menu, Player player, Inventory inventory, BiMap<MenuItem, Integer> items, OpenAnimation.Animation opening) {
        this.plugin = plugin;
        this.menu = menu;
        this.player = player;
        this.inventory = inventory;
        this.items = items;
        this.opening = opening;
        title = inventory.getTitle();
    }

    public Player getPlayer() {
        return player;
    }

    public void updateTitle(String title) {
        if (!title.equals(this.title)) {
            this.title = title;
            TITLE_UPDATER.update(player, inventory, title);
        }
    }

    public AnimatedMenuPlugin getPlugin() {
        return plugin;
    }

    public AbstractMenu getMenu() {
        return menu;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public MenuItem getItem(int slot) {
        return items.inverse().get(slot);
    }

    public boolean isOpening() {
        return opening != null;
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

    public static class Property<T> {

        public static <T> Property<T> of() {
            return new Property<>(null);
        }

        public static <T> Property<T> of(T defaultValue) {
            return new Property<>(defaultValue);
        }

        private final T defaultValue;

        private Property(T defaultValue) {
            this.defaultValue = defaultValue;
        }

        private int hash;

        @Override
        public int hashCode() {
            return hash != 0 ? hash : (hash = super.hashCode());
        }
    }
}
