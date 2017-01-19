package me.megamichiel.animatedmenu.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;
import java.util.function.Consumer;

public class MenuGrid {

    private final AbstractMenu menu;
    private MenuItem[] items;
    private int size = 0;
    
    MenuGrid(AbstractMenu menu, int size) {
        this.menu = menu;
        items = new MenuItem[size];
    }

    void sortSlots() {
        MenuItem[] solid = new MenuItem[size],
                dynamic = new MenuItem[size];
        int solidSize = 0, dynamicSize = 0;
        for (int i = 0; i < size; i++) {
            if (items[i].getInfo().hasDynamicSlot()) dynamic[dynamicSize++] = items[i];
            else solid[solidSize++] = items[i];
        }
        System.arraycopy(solid, 0, items, 0, solidSize);
        System.arraycopy(dynamic, 0, items, solidSize, dynamicSize);
    }
    
    public void click(Player p, ClickType click, int slot) {
        menu.click(p, slot, click);
    }
    
    public MenuItem addItem(MenuItemInfo item) {
        if (size == items.length)
            items = Arrays.copyOf(items, size + 9);
        return items[size++] = new MenuItem(item);
    }

    public void removeItem(MenuItem item) {
        removeItem(item.getInfo());
    }

    public void removeItem(MenuItemInfo item) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].getInfo() == item) {
                --size;
                while (i < size)
                    items[i] = items[++i];
                items[i] = null;
                break;
            }
        }
    }

    public void clear() {
        while (size > 0) items[--size] = null;
    }

    public MenuItem[] getItems() {
        return items;
    }

    public void forEachItem(Consumer<? super MenuItem> action) {
        for (int i = 0; i < size; i++)
            action.accept(items[i]);
    }

    public int getSize() {
        return size;
    }
}
