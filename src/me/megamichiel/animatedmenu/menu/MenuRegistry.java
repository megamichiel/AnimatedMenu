package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.menu.item.IMenuItem;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class MenuRegistry implements Iterable<AbstractMenu>, Runnable {

    private final Set<IMenuProvider<?>> providers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    @Override
    public void run() {
        AbstractMenu.GridEntry[] gridEntries;
        AbstractMenu.GridEntry head = null, gridEntry = null, empty;
        int slot, value, i;

        MenuSession session;
        BooleanSupplier open;
        MenuSession.GridEntry[] sessEntries;
        Map<IMenuItem, MenuSession.GridEntry> slots;

        MenuSession.GridEntry sessEntry, current;

        Player player;
        Inventory inv;
        ItemStack stack;

        for (IMenuProvider<?> provider : providers) {
            for (AbstractMenu menu : provider) {
                try {
                    menu.tick();
                } catch (Exception ex) {
                    menu.nagger.nag("An error occurred on ticking " + menu.getName() + ":");
                    menu.nagger.nag(ex);
                }

                for (i = value = (gridEntries = menu.grid.entries).length; --i >= 0; ) {
                    switch ((gridEntry = gridEntries[i]).tick = gridEntry.item.tick()) {
                        case IMenuItem.REMOVE: // Remove item
                            System.arraycopy(value == gridEntries.length ? (gridEntries = gridEntries.clone()) : gridEntries, i + 1, gridEntries, i, --value - i); // Shift items to the right 1 position left
                        default:
                            gridEntry.next = head;
                            head = gridEntry;
                        case 0: // Nothing needs updating
                    }
                }

                if ((i = menu.grid.add.size()) > 0 || value != gridEntries.length) { // Items have been removed or added
                    for (System.arraycopy(gridEntries, 0, gridEntries = new AbstractMenu.GridEntry[value + i], 0, value); --i >= 0; gridEntries[slot] = new AbstractMenu.GridEntry(gridEntry.item, 0)) {
                        System.arraycopy(gridEntries, slot = (gridEntry = menu.grid.add.poll()).tick == 0 ? value : gridEntry.tick - 1, gridEntries, slot + 1, value++ - slot);
                    }
                    menu.grid.entries = gridEntries;
                }

                if ((i = (empty = menu.emptyItem) == null ? 0 : (empty.tick = empty.item.tick())) == IMenuItem.REMOVE) {
                    empty = menu.emptyItem = null;
                }

                for (Map.Entry<Player, MenuSession> mapEntry : menu.sessions.entrySet()) {
                    if ((open = (session = mapEntry.getValue()).openAnimation) != null) {
                        if (!open.getAsBoolean()) {
                            return;
                        }
                        session.openAnimation = null;
                    }

                    player = mapEntry.getKey();
                    sessEntries = session.entries;
                    slots = session.slots;
                    inv = session.inventory;

                    if (((stack = session.emptyStack) != null && empty == null) || (i & IMenuItem.UPDATE_ITEM) != 0) { // Empty item change
                        try {
                            stack = session.emptyStack = empty == null ? null : empty.item.getItem(player, session, stack, stack == null ? 0 : i);
                        } catch (Exception ex) {
                            menu.nagger.nag("Failed to update empty item in menu " + menu.getName() + "!");
                            menu.nagger.nag(ex);
                        }
                        for (slot = sessEntries.length; slot > 0; ) {
                            if (sessEntries[--slot] == null) {
                                inv.setItem(slot, stack);
                            }
                        }
                    }

                    for (gridEntry = head; gridEntry != null; gridEntry = gridEntry.next) {
                        if ((value = gridEntry.tick) == IMenuItem.REMOVE) { // Item has requested remove
                            if ((sessEntry = session.slots.remove(gridEntry.item)) != null && (slot = sessEntry.slot) != IMenuItem.NO_SLOT) {
                                sessEntries[slot] = null;
                                inv.setItem(slot, session.emptyStack);
                            }
                            continue;
                        }

                        slot = (sessEntry = session.entry = slots.computeIfAbsent(gridEntry.item, MenuSession.GridEntry::new)).slot;
                        if ((value & IMenuItem.UPDATE_ITEM) != 0) {
                            try {
                                sessEntry.stack = gridEntry.item.getItem(player, session, sessEntry.stack, value);
                            } catch (Exception ex) {
                                menu.nagger.nag("Failed to get item of " + gridEntry.item + " in menu " + menu.getName() + "!");
                                menu.nagger.nag(ex);
                            }
                            if (slot != IMenuItem.NO_SLOT) {
                                if (sessEntry.stack != null) {
                                    sessEntries[slot] = sessEntry;
                                    inv.setItem(slot, sessEntry.stack);
                                } else if (sessEntries[slot] == sessEntry) {
                                    sessEntries[slot] = null;
                                    inv.setItem(slot, null);
                                }
                            }
                        }
                        if ((value & IMenuItem.UPDATE_WEIGHT) != 0) {
                            try {
                                sessEntry.weight = gridEntry.item.getWeight(player, session);
                            } catch (Exception ex) {
                                menu.nagger.nag("Failed to update weight of " + gridEntry.item + " in menu " + menu.getName() + "!");
                                menu.nagger.nag(ex);
                            }
                        }
                        if ((value & IMenuItem.UPDATE_SLOT) != 0) {
                            try {
                                slot = (slot = gridEntry.item.getSlot(player, session)) < 0 || (slot & IMenuItem.NO_SLOT) >= sessEntries.length ? IMenuItem.NO_SLOT : slot;
                            } catch (Exception ex) {
                                menu.nagger.nag("Failed to update slot of " + gridEntry.item + " in menu " + menu.getName() + "!");
                                menu.nagger.nag(ex);
                            }

                            if (slot != sessEntry.slot && sessEntry.slot != IMenuItem.NO_SLOT && sessEntries[sessEntry.slot] == sessEntry) {
                                sessEntries[sessEntry.slot] = null;
                                inv.setItem(sessEntry.slot, session.emptyStack);
                            }
                            if ((sessEntry.slot = slot) != IMenuItem.NO_SLOT && (stack = sessEntry.stack) != null) {
                                current = sessEntries[value = slot & IMenuItem.NO_SLOT];
                                (sessEntries[value] = sessEntry).slot = value;

                                if ((slot & IMenuItem.SLOT_SHIFT_RIGHT) != 0) {
                                    for (sessEntry = current; ++value < sessEntries.length && sessEntry != null; (sessEntries[value] = sessEntry).slot = value, sessEntry = current) {
                                        current = sessEntries[value];
                                    }
                                } else if ((slot & IMenuItem.SLOT_SHIFT_LEFT) != 0) {
                                    for (sessEntry = current; --value >= 0 && sessEntry != null; (sessEntries[value] = sessEntry).slot = value, sessEntry = current) {
                                        current = sessEntries[value];
                                    }
                                }

                                inv.setItem(sessEntry == null ? slot : slot & (sessEntry.slot = IMenuItem.NO_SLOT), stack);
                            }
                        }
                    }

                    session.entry = null;
                }
            }
        }
    }

    /**
     * Registers a new IMenuProvider
     * @param provider The provider to register.
     *
     * @throws NullPointerException if <i>provider</i> is null
     */
    public void registerProvider(IMenuProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        providers.add(provider);
    }

    /**
     * Unregisters an IMenuProvider
     */
    public void unregisterProvider(IMenuProvider provider) {
        providers.remove(provider);
    }

    /**
     * Returns a set of all currently loaded menus
     */
    public Collection<AbstractMenu> getMenus() {
        List<AbstractMenu> list = new ArrayList<>();
        for (IMenuProvider<?> provider : providers) {
            for (AbstractMenu menu : provider) {
                list.add(menu);
            }
        }
        return list;
    }

    /**
     * Checks whether a menu is registered in this registry
     * @param menu the menu to check
     * @return true if the menu is registered, false otherwise
     */
    protected boolean contains(AbstractMenu menu) {
        String name = menu.getName();
        for (IMenuProvider<?> provider : providers) {
            if (provider.getMenu(name) == menu) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a menu by name
     * @param name the name of the menu
     * @return the menu by name, or null if none was found
     */
    public AbstractMenu getMenu(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        AbstractMenu menu;
        for (IMenuProvider<?> provider : providers) {
            if ((menu = provider.getMenu(name)) != null) {
                return menu;
            }
        }
        return null;
    }

    /**
     * Returns the current menu session of a player
     * @param player The player to get the session of
     * @return The player's current menu session, or <i>null</i> if they don't have a menu open
     */
    public MenuSession getSession(HumanEntity player) {
        InventoryHolder holder;
        return (holder = player.getOpenInventory().getTopInventory().getHolder()) instanceof MenuSession ? (MenuSession) holder : null;
    }

    /**
     * Get a new iterator of all menus
     */
    @Override
    public Iterator<AbstractMenu> iterator() {
        return new Iterator<AbstractMenu>() {

            final Iterator<IMenuProvider<?>> provider = providers.iterator();
            Iterator<? extends AbstractMenu> menu = provider.hasNext() ? provider.next().iterator() : Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                for (; !menu.hasNext(); menu = provider.next().iterator()) {
                    if (!provider.hasNext()) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public AbstractMenu next() {
                while (!menu.hasNext()) {
                    menu = provider.next().iterator();
                }
                return menu.next();
            }
        };
    }

    @Override
    public void forEach(Consumer<? super AbstractMenu> action) {
        Iterator<? extends AbstractMenu> it;
        for (IMenuProvider<?> provider : providers) {
            for (it = provider.iterator(); it.hasNext(); ) {
                action.accept(it.next());
            }
        }
    }
}
