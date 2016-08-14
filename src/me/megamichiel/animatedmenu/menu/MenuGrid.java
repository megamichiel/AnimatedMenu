package me.megamichiel.animatedmenu.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;

public class MenuGrid {

    private final AbstractMenu menu;
    private IMenuItem[] items;
    private int size = 0;
    
    MenuGrid(AbstractMenu menu, int size) {
        this.menu = menu;
        items = new IMenuItem[size];
    }

    void sortSlots() {
        IMenuItem[] solid = new IMenuItem[size],
                dynamic = new IMenuItem[size];
        int solidSize = 0, dynamicSize = 0;
        for (int i = 0; i < size; i++) {
            if (items[i].hasDynamicSlot()) dynamic[dynamicSize++] = items[i];
            else solid[solidSize++] = items[i];
        }
        System.arraycopy(solid, 0, items, 0, solidSize);
        System.arraycopy(dynamic, 0, items, solidSize, dynamicSize);
    }
    
    public void click(Player p, ClickType click, int slot) {
        IMenuItem item = menu.getItem(p, slot);
        if (item != null) item.click(p, click);
    }
    
    public void addItem(IMenuItem item) {
        if (size == items.length)
            items = Arrays.copyOf(items, size + 9);
        items[size++] = item;
    }

    public void clear() {
        while (size > 0) items[--size] = null;
    }

    public IMenuItem[] getItems() {
        return items;
    }

    public int getSize() {
        return size;
    }
}
