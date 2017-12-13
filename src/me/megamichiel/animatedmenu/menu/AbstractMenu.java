package me.megamichiel.animatedmenu.menu;

import com.google.common.collect.ImmutableMap;
import me.megamichiel.animatedmenu.animation.OpenAnimation;
import me.megamichiel.animatedmenu.menu.item.IMenuItem;
import me.megamichiel.animatedmenu.util.Delay;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.util.collect.ConcurrentArrayList;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static me.megamichiel.animatedmenu.menu.item.IMenuItem.*;

public abstract class AbstractMenu {

    private final Nagger nagger;
    private final Grid grid = new Grid();

    private final String name;
    private final Type type;

    private final Map<Player, MenuSession> sessions = new ConcurrentHashMap<>();

    private Function<? super Player, ? extends OpenAnimation> openAnimation;

    private IMenuItem emptyItem;
    private boolean emptyItemRemoved, singleEmptyItem;

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
        return emptyItem;
    }

    public void setEmptyItem(IMenuItem info) {
        if (emptyItem != (emptyItem = info) && info == null) {
            emptyItemRemoved = true;
        }
    }

    public void setSingleEmptyItem(boolean singleEmptyItem) {
        this.singleEmptyItem = singleEmptyItem;
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

    private void setup(Player who, MenuSession session) {
        Inventory inv = session.getInventory();
        MenuSession.GridEntry[] entries = session.entries;
        int size = entries.length;

        IMenuItem emptyItem = this.emptyItem;
        ItemStack emptyStack;
        if (emptyItem == null || !singleEmptyItem) {
            emptyStack = null;
        } else {
            ItemStack itemStack = null;
            try {
                itemStack = emptyItem.getItem(who, session, null, -1);
            } catch (Exception ex) {
                nagger.nag("Failed to load empty item " + emptyItem + " in menu " + name + "!");
                nagger.nag(ex);
            }
            emptyStack = itemStack;
        }

        Function<? super Player, ? extends OpenAnimation> openAnimation = this.openAnimation;
        OpenAnimation anim = openAnimation == null ? null : openAnimation.apply(who);

        int slot;
        for (IMenuItem item : this.grid) {
            ItemStack stack = null;
            try {
                stack = item.getItem(who, session, null, -1);
            } catch (Exception ex) {
                nagger.nag("Failed to load item " + item + " in menu " + name + "!");
                nagger.nag(ex);
            }
            if (stack != null && (slot = session.getSlot(item, stack, -1)) >= 0 && slot < size && anim == null) {
                inv.setItem(slot, stack);
            }
        }

        if (anim != null) {
            session.setOpenAnimation(() -> anim.tick(i -> {
                MenuSession.GridEntry entry = entries[i];
                if (entry != null) {
                    inv.setItem(i, entry.stack);
                } else if (emptyStack != null) {
                    inv.setItem(i, emptyStack);
                } else if (emptyItem != null) {
                    try {
                        inv.setItem(i, emptyItem.getItem(who, session, null, -1));
                    } catch (Exception ex) {
                        nagger.nag("Failed to load empty item " + emptyItem + "in menu " + name + "!");
                        nagger.nag(ex);
                    }
                }
            }));
        } else if (emptyItem != null) {
            for (int i = 0; i < size; ++i) {
                if (entries[i] == null) {
                    if (emptyStack == null) {
                        try {
                            inv.setItem(i, emptyItem.getItem(who, session, null, -1));
                        } catch (Exception ex) {
                            nagger.nag("Failed to load empty item " + emptyItem + " in menu " + name + "!");
                            nagger.nag(ex);
                        }
                    } else {
                        inv.setItem(i, emptyStack);
                    }
                }
            }
        }

        session.updateInventory = true;

        sessions.put(who, session);
    }

    protected MenuSession setup(Player who, Function<MenuSession, Inventory> inventory, Consumer<? super MenuSession> action) {
        MenuSession session = new MenuSession(this, nagger, who, inventory);
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

    public void forEachSession(Consumer<? super MenuSession> action) {
        sessions.values().forEach(action);
    }

    public void forEachViewer(Consumer<? super Player> action) {
        sessions.keySet().forEach(action);
    }

    public void tick() {
        IMenuItem emptyItem = this.emptyItem; // Consistency!

        List<ItemTick> items = new ArrayList<>();

        boolean gridChanged = false, emptyRemoved = emptyItemRemoved;
        int emptyTick = emptyItem == null ? -1 : emptyItem.tick();
        for (IMenuItem item; (item = grid.removed.poll()) != null; ) {
            items.add(new ItemTick(item, -1));
        }
        {
            int tick;
            for (IMenuItem item : grid.items) {
                items.add(new ItemTick(item, tick = item.tick() & 0x7FFFFFFF));
                gridChanged |= tick != 0;
            }
        }

        if (!gridChanged && emptyTick == 0 && !emptyRemoved) { // Nothing's changed, only update open animations
            sessions.values().forEach(MenuSession::tick);
            return;
        }

        sessions.forEach((player, session) -> {
            if (session.tick()) {
                return;
            }
            MenuSession.GridEntry[] entries = session.entries;
            Inventory inv = session.getInventory();
            Map<IMenuItem, Integer> slots = session.slots;

            ItemStack stack, newStack;
            int slot;
            for (ItemTick frame : items) {
                IMenuItem item = frame.item;
                int tick = frame.tick;
                Integer old;
                if (tick == -1) {
                    if ((old = slots.remove(item)) != null) {
                        entries[old] = null;
                        try {
                            inv.setItem(old, emptyItem == null ? null : emptyItem.getItem(player, session, null, -1));
                        } catch (Exception ex) {
                            nagger.nag("Failed to load empty item " + emptyItem + " in menu " + name + "!");
                        }
                    }
                } else if ((old = slots.get(item)) != null && (stack = inv.getItem(slot = old)) != null) { // Item exists in the inventory

                    try {
                        newStack = (tick & UPDATE_ITEM) == 0 ? stack : item.getItem(player, session, stack, tick);
                    } catch (Exception ex) {
                        nagger.nag("Failed to tick item " + item + " in menu " + name + "!");
                        nagger.nag(ex);
                        newStack = stack;
                    }

                    if (newStack == null) {
                        inv.setItem(slot, null);
                        slots.remove(item);
                        entries[slot] = null;
                    } else if (slot == (slot = session.getSlot(item, stack, tick))) {
                        if (newStack != stack) {
                            inv.setItem(slot, newStack);
                        }
                    } else {
                        inv.setItem(old, null);
                        if (slot != -1) {
                            inv.setItem(slot, newStack);
                        } else if ((old = slots.remove(item)) != null) {
                            entries[old] = null;
                        }
                    }
                } else {
                    try {
                        stack = item.getItem(player, session, null, tick);
                    } catch (Exception ex) {
                        nagger.nag("Failed to load item " + item + " in menu " + name + "!");
                        nagger.nag(ex);
                        continue;
                    }
                    if (stack != null && (slot = session.getSlot(item, stack, tick)) != -1) {
                        inv.setItem(slot, stack);
                    }
                }
            }
            if (emptyItem != null) {
                for (int i = inv.getSize(); --i >= 0; ) {
                    if (entries[i] == null) {
                        if ((stack = inv.getItem(i)) != null) {
                            try {
                                if (emptyTick > 0 && stack != emptyItem.getItem(player, session, stack, emptyTick)) {
                                    inv.setItem(i, stack);
                                }
                            } catch (Exception ex) {
                                nagger.nag("Failed to update empty item in menu " + name + "!");
                                nagger.nag(ex);
                            }
                        } else {
                            try {
                                inv.setItem(i, emptyItem.getItem(player, session, null, -1));
                            } catch (Exception ex) {
                                nagger.nag("Failed to load empty item in menu " + name + "!");
                                nagger.nag(ex);
                            }
                        }
                    }
                }
            } else if (emptyRemoved) {
                for (int i = inv.getSize(); --i >= 0; ) {
                    if (entries[i] == null) {
                        inv.setItem(i, null);
                    }
                }
            }
        });
    }

    public static final class Grid implements Iterable<IMenuItem> {

        private final List<IMenuItem> items = new ConcurrentArrayList<>();
        private final Queue<IMenuItem> removed = new ConcurrentLinkedQueue<>();

        private Grid() { }

        /**
         * Returns the size of this menu grid
         */
        public int size() {
            return items.size();
        }

        /**
         * Returns whether this menu grid has no items in it
         */
        public boolean isEmpty() {
            return items.isEmpty();
        }

        /**
         * Adds an item to the end of this grid
         *
         * @param info The info of the item to add
         */
        public void add(IMenuItem info) {
            items.add(info);
        }

        /**
         * Adds an item at a specific index in this menu grid
         *
         * @param index the index to add the item at
         * @param info The info of the item to add
         */
        public void add(int index, IMenuItem info) {
            items.add(index, info);
        }

        /**
         * Adds an item to the front of this menu grid
         *
         * @param info The info of the item to add
         */
        public void addFirst(IMenuItem info) {
            items.add(0, info);
        }

        /**
         * Adds an item relative to another item.<br/>
         * If the target item is not in the menu, <i>false</i> will be returned
         *
         * @param base The item to add the item relative to
         * @param after If true, <i>info</i> will be added after <i>base</i>. Otherwise, it will be added before it.
         * @param info The item to add
         */
        public boolean add(IMenuItem base, boolean after, IMenuItem info) {
            int index = items.indexOf(base);
            if (index == -1) {
                return false;
            }
            items.add(after ? index + 1 : index, info);
            return true;
        }

        /**
         * Adds an item relative to another item.<br/>
         * If the predicate did not find any matching item, <i>false</i> is returned
         *
         * @param predicate The Predicate used to match the item to find
         * @param after If true, <i>info</i> will be added after <i>base</i>. Otherwise, it will be added before it.
         * @param info The item to add
         */
        public boolean add(Predicate<? super IMenuItem> predicate, boolean after, IMenuItem info) {
            List<IMenuItem> items = this.items;
            for (int i = 0, size = items.size(); i < size; ++i) {
                if (predicate.test(items.get(i))) {
                    items.add(after ? i + 1 : i, info);
                    return true;
                }
            }
            return false;
        }

        /**
         * Removes an item from this menu grid
         *
         * @param info The info of the item to remove from this grid
         * @return True if this menu grid contained <i>info</i>. False otherwise
         */
        public boolean remove(IMenuItem info) {
            if (items.remove(info)) {
                removed.add(info);
            }
            return false;
        }

        /**
         * Checks if this menu grid contains an item
         *
         * @param info The info to find
         * @return True if this menu grid contains <i>info</i>. False otherwise
         */
        public boolean contains(IMenuItem info) {
            return items.contains(info);
        }

        /**
         * Clears this Grid, removing all of its items
         */
        public void clear() {
            removed.addAll(items);
            items.clear();
        }

        @Override
        public Iterator<IMenuItem> iterator() {
            Iterator<IMenuItem> it = items.iterator();
            return new Iterator<IMenuItem>() {
                IMenuItem next;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public IMenuItem next() {
                    return next = it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                    removed.add(next);
                    next = null;
                }
            };
        }

        @Override
        public void forEach(Consumer<? super IMenuItem> action) {
            items.forEach(action);
        }

        @Override
        public Spliterator<IMenuItem> spliterator() {
            return items.spliterator();
        }

    }

    static class ItemTick {

        final IMenuItem item;
        final int tick;

        ItemTick(IMenuItem item, int tick) {
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
