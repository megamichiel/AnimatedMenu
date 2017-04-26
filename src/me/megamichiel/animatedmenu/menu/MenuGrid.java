package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.menu.item.ItemInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MenuGrid implements Iterable<MenuItem> {

    private final AbstractMenu menu;
    private final List<MenuItem> items = new CopyOnWriteArrayList<>();

    private int itemId = 0;
    
    MenuGrid(AbstractMenu menu) {
        this.menu = menu;
    }

    boolean tick(boolean base) {
        for (MenuItem item : items) {
            if (item.tick() && !base) {
                base = true;
            }
        }
        return base;
    }

    /**
     * Adds an item to this menu depending on ItemInfo#hasFixedSlot.<br/>
     * If it returns false, #addLast will be used. Otherwise, it will be added after the last item with fixed slot (or the front)
     *
     * @param info The info of the item to add
     * @return The newly created MenuItem
     */
    public MenuItem add(ItemInfo info) {
        if (info.hasFixedSlot()) {
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).getInfo().hasFixedSlot()) {
                    MenuItem item = new MenuItem(info, itemId++);
                    items.add(i, item);
                    return item;
                }
            }
        }
        return addLast(info);
    }

    /**
     * Adds an item to the end of this menu grid
     *
     * @param info The info of the item to add
     * @return The newly created MenuItem
     */
    public MenuItem addLast(ItemInfo info) {
        MenuItem item = new MenuItem(info, itemId++);
        items.add(item);
        return item;
    }

    /**
     * Adds an item to the front of this menu grid
     *
     * @param info The info of the item to add
     * @return The newly created MenuItem
     */
    public MenuItem addFirst(ItemInfo info) {
        MenuItem item = new MenuItem(info, itemId++);
        items.add(0, item);
        return item;
    }

    /**
     * Adds an item before another item.<br/>
     * If the target item is not in the menu, <i>null</i> will be returned
     *
     * @param info The info of the item to add
     * @return The newly created MenuItem
     */
    public MenuItem addBefore(MenuItem item, ItemInfo info) {
        int index = items.indexOf(item);
        if (index < 0) return null;
        MenuItem newItem = new MenuItem(info, itemId++);
        items.add(index, newItem);
        return newItem;
    }

    /**
     * Adds an item before after item.<br/>
     * If the target item is not in the menu, <i>null</i> will be returned
     *
     * @param info The info of the item to add
     * @return The newly created MenuItem
     */
    public MenuItem addAfter(MenuItem item, ItemInfo info) {
        int index = items.indexOf(item);
        if (index < 0) return null;
        MenuItem newItem = new MenuItem(info, itemId++);
        items.add(index + 1, newItem);
        return newItem;
    }

    /**
     * Removes an item from this menu grid
     *
     * @param info The info of the item to remove from this grid
     * @return True if this menu grid contained <i>info</i>. False otherwise
     */
    public boolean remove(ItemInfo info) {
        for (MenuItem item : items) {
            if (item.getInfo() == info) {
                items.remove(item);
                menu.itemRemoved(item);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes an item from this menu grid
     *
     * @param item The item to remove from this grid
     * @return True if this menu grid contained <i>info</i>. False otherwise
     */
    public boolean remove(MenuItem item) {
        if (items.remove(item)) {
            menu.itemRemoved(item);
            return true;
        }
        return false;
    }

    /**
     * Returns the MenuItem associated with a given ItemInfo
     *
     * @param info The ItemInfo that the target MenuItem has
     * @return The MenuItem that holds the given ItemInfo, or null if not found
     */
    public MenuItem getItem(ItemInfo info) {
        for (MenuItem item : items) {
            if (item.getInfo() == info) {
                return item;
            }
        }
        return null;
    }

    /**
     * Checks if this menu grid contains an item
     *
     * @param info The info to find
     * @return True if this menu grid contains <i>info</i>. False otherwise
     */
    public boolean contains(ItemInfo info) {
        for (MenuItem item : items) {
            if (item.getInfo() == info) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears this MenuGrid, removing all of its items
     */
    public void clear() {
        items.forEach(menu::itemRemoved);
        items.clear();
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public Iterator<MenuItem> iterator() {
        return items.iterator();
    }

    @Override
    public void forEach(Consumer<? super MenuItem> action) {
        items.forEach(action);
    }

    @Override
    public Spliterator<MenuItem> spliterator() {
        return items.spliterator();
    }
}
