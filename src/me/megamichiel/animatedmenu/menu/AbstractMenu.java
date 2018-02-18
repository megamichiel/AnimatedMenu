package me.megamichiel.animatedmenu.menu;

import com.google.common.collect.ImmutableMap;
import me.megamichiel.animatedmenu.menu.item.IMenuItem;
import me.megamichiel.animatedmenu.util.Delay;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.Math.min;
import static org.bukkit.util.NumberConversions.floor;

public abstract class AbstractMenu {

    final Nagger nagger;
    final Grid grid = new Grid();

    private final String name;
    private final Type type;

    final Map<Player, MenuSession> sessions = new ConcurrentHashMap<>();

    private Function<? super Player, ? extends OpenAnimation> openAnimation;

    GridEntry emptyItem;

    private Delay clickDelay;

    protected AbstractMenu(Nagger nagger, String name, Type type) {
        this.nagger = nagger;
        this.name = name;
        this.type = type;
    }

    public void setOpenAnimation(Function<? super Player, ? extends OpenAnimation> openAnimation) {
        this.openAnimation = openAnimation;
    }

    public void setClickDelay(Delay delay) {
        clickDelay = delay;
    }

    public Delay getClickDelay() {
        return clickDelay;
    }

    public IMenuItem getEmptyItem() {
        GridEntry entry = emptyItem;
        return entry == null ? null : entry.item;
    }

    public void setEmptyItem(IMenuItem info) {
        emptyItem = info == null ? null : new GridEntry(info, 0);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Grid getGrid() {
        return grid;
    }

    protected MenuSession removeViewer(Player player) {
        return sessions.remove(player);
    }

    private void setup(Player player, MenuSession session) {
        Inventory inv = session.inventory;
        MenuSession.GridEntry[] sessEntries = session.entries;

        GridEntry emptyItem = this.emptyItem;
        ItemStack emptyStack = null;
        if (emptyItem != null) {
            try {
                emptyStack = session.emptyStack = emptyItem.item.getItem(player, session, null, emptyItem.tick);
            } catch (Exception ex) {
                nagger.nag("Failed to load empty item " + emptyItem + " in menu " + getName() + "!");
                nagger.nag(ex);
            }
        }

        int slot, value;

        for (GridEntry gridEntry : grid.entries) {
            MenuSession.GridEntry sessEntry = session.entry = new MenuSession.GridEntry(gridEntry.item);
            try {
                sessEntry.stack = gridEntry.item.getItem(player, session, null, gridEntry.tick);
            } catch (Exception ex) {
                nagger.nag("Failed to load item of " + gridEntry.item + " in menu " + getName() + "!");
                nagger.nag(ex);
            }
            try {
                sessEntry.weight = gridEntry.item.getWeight(player, session);
            } catch (Exception ex) {
                nagger.nag("Failed to load weight of " + gridEntry.item + " in menu " + getName() + "!");
                nagger.nag(ex);
            }
            try {
                sessEntry.slot = (slot = gridEntry.item.getSlot(player, session)) < 0 || (slot & IMenuItem.NO_SLOT) >= sessEntries.length ? IMenuItem.NO_SLOT : slot;
            } catch (Exception ex) {
                nagger.nag("Failed to load slot of " + gridEntry.item + " in menu " + getName() + "!");
                nagger.nag(ex);
            }

            session.entry = null;
            session.slots.put(gridEntry.item, sessEntry);

            if (sessEntry.stack != null && (slot = sessEntry.slot) != IMenuItem.NO_SLOT) {
                MenuSession.GridEntry current = sessEntries[value = slot & IMenuItem.NO_SLOT];
                (sessEntries[value] = sessEntry).slot = value;

                if ((slot & IMenuItem.SLOT_SHIFT_RIGHT) != 0) {
                    for (; ++slot < sessEntries.length && sessEntry != null; (sessEntries[value] = sessEntry).slot = value, sessEntry = current) {
                        current = sessEntries[value];
                    }
                } else if ((slot & IMenuItem.SLOT_SHIFT_LEFT) != 0) {
                    for (; --slot >= 0 && sessEntry != null; (sessEntries[value] = sessEntry).slot = value, sessEntry = current) {
                        current = sessEntries[value];
                    }
                }
                if (current != null) {
                    current.slot = IMenuItem.NO_SLOT;
                }
            }
        }

        Function<? super Player, ? extends OpenAnimation> func = this.openAnimation;
        OpenAnimation anim;

        if (func == null || (anim = func.apply(player)) == null) {
            MenuSession.GridEntry entry;
            for (int i = 0; i < sessEntries.length; i++) {
                inv.setItem(i, (entry = sessEntries[i]) == null ? emptyStack : entry.stack);
            }
        } else {
            ItemStack empty = emptyStack;
            session.openAnimation = new BooleanSupplier() {
                final byte[][] frames = anim.getFrames();
                double speed = anim.getSpeed(), frame;

                @Override
                public boolean getAsBoolean() {
                    int i = floor(frame), to = min(floor(frame += speed), frames.length);
                    MenuSession.GridEntry entry;
                    while (i < to) {
                        for (int j : frames[i++]) {
                            if (j >= 0) {
                                inv.setItem(j, (entry = sessEntries[j]) == null ? empty : entry.stack);
                            }
                        }
                    }
                    return i >= frames.length;
                }
            };
        }

        session.updateInventory = true;

        sessions.put(player, session);
    }

    protected MenuSession setup(Player who, Function<MenuSession, Inventory> inventory, Consumer<? super MenuSession> action) {
        MenuSession session = new MenuSession(this, nagger, inventory);
        if (action != null) {
            action.accept(session);
        }
        setup(who, session);
        return session;
    }

    public MenuSession getSession(Player player) {
        return sessions.get(player);
    }

    public Set<Player> getViewers() {
        return new HashSet<>(sessions.keySet());
    }

    public boolean isViewing(Player player) {
        return sessions.containsKey(player);
    }

    public void forEach(BiConsumer<? super Player, ? super MenuSession> action) {
        sessions.forEach(action);
    }

    public void forEachSession(Consumer<? super MenuSession> action) {
        sessions.values().forEach(action);
    }

    public void forEachViewer(Consumer<? super Player> action) {
        sessions.keySet().forEach(action);
    }

    public void tick() { }

    public static final class Grid implements Iterable<IMenuItem> {

        GridEntry[] entries = new GridEntry[0];
        final Queue<GridEntry> add = new ConcurrentLinkedQueue<>();

        private Grid() { }

        /**
         * Returns the size of this menu grid
         */
        public int size() {
            return entries.length;
        }

        /**
         * Returns whether this menu grid has no items in it
         */
        public boolean isEmpty() {
            return entries.length == 0;
        }

        /**
         * Adds an item to the end of this grid
         *
         * @param item The info of the item to add
         */
        public void add(IMenuItem item) {
            add.add(new GridEntry(item, 0));
        }

        /**
         * Adds an item at a specific index in this menu grid
         *
         * @param index the index to add the item at
         * @param item The info of the item to add
         */
        public void add(int index, IMenuItem item) {
            if (index < 0) {
                throw new IndexOutOfBoundsException("index < 0");
            }

            add.add(new GridEntry(item, index + 1));
        }

        public void addAll(Collection<? extends IMenuItem> items) {
            for (IMenuItem item : items) {
                add.add(new GridEntry(item, 0));
            }
        }

        /**
         * Clears this Grid, removing all of its items
         */
        public void clear() {
            for (GridEntry entry : entries) {
                entry.tick = IMenuItem.REMOVE;
            }
        }

        @Override
        public Iterator<IMenuItem> iterator() {
            return new Iterator<IMenuItem>() {
                final GridEntry[] entries = Grid.this.entries;
                int index; GridEntry current;

                @Override
                public boolean hasNext() {
                    return index < entries.length;
                }

                @Override
                public IMenuItem next() {
                    if (index >= entries.length) {
                        throw new NoSuchElementException();
                    }
                    return (current = entries[index++]).item;
                }

                @Override
                public void remove() {
                    if (current == null) {
                        throw new IllegalStateException();
                    }
                    current.tick = IMenuItem.REMOVE;
                    current = null;
                }
            };
        }

        @Override
        public void forEach(Consumer<? super IMenuItem> action) {
            for (GridEntry entry : entries) {
                if (entry.tick != IMenuItem.REMOVE) {
                    action.accept(entry.item);
                }
            }
        }
    }

    static class GridEntry {

        final IMenuItem item;
        int tick;

        GridEntry next;

        GridEntry(IMenuItem item, int tick) {
            this.item = item;
            this.tick = tick;
        }
    }

    public static class Type {

        public static final Type HOPPER = new Type(InventoryType.HOPPER, "minecraft:hopper", 5, 1);
        public static final Type DISPENSER = new Type(InventoryType.DISPENSER, "minecraft:dispenser", 3, 3);
        public static final Type DROPPER = new Type(InventoryType.DROPPER, "minecraft:dropper", 3, 3);
        public static final Type WORKBENCH = new Type(InventoryType.WORKBENCH, "minecraft:crafting_table", 3, 3);

        private static final Type[] VALUES = new Type[] { HOPPER, DISPENSER, DROPPER, WORKBENCH};

        private static final Type[] CHEST = new Type[6];

        private static final Map<String, Type> BY_NAME;

        static {
            ImmutableMap.Builder<String, Type> builder = ImmutableMap.builder();
            for (Type value : VALUES) {
                builder.put(value.inventoryType.name(), value);
            }
            BY_NAME = builder.build();
        }

        public static Type chest(int rows) {
            Type type = CHEST[rows - 1];
            return type != null ? type : (CHEST[rows - 1] = new Type(InventoryType.CHEST, "minecraft:chest", 9, rows));
        }

        public static Type fromName(String name) {
            return BY_NAME.get(name);
        }

        public static Type[] values() {
            return VALUES.clone();
        }

        private final InventoryType inventoryType;
        private final String nmsName;
        private final int width, height, size;

        public Type(InventoryType type, String nmsName, int width, int height) {
            inventoryType = type;
            this.nmsName = nmsName;
            size = (this.width = width) * (this.height = height);
        }

        public boolean isChest() {
            return inventoryType == InventoryType.CHEST;
        }

        public InventoryType getInventoryType() {
            return inventoryType;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getSize() {
            return size;
        }

        public String getNmsName() {
            return nmsName;
        }
    }
}
