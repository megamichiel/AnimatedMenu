package me.megamichiel.animatedmenu.menu;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class MenuGrid implements Iterable<MenuItem> {

    private final AbstractMenu menu;

    private MenuItem head, tail;
    private int size = 0, itemId = 0;
    private boolean dynamicSlots = false;
    
    MenuGrid(AbstractMenu menu) {
        this.menu = menu;
    }
    
    public MenuItem add(ItemInfo info) {
        MenuItem item = new MenuItem(info, itemId++);
        ++size;
        if (head == null) {
            if (info.hasDynamicSlot()) dynamicSlots = true;
            return head = tail = item;
        }
        if (info.hasDynamicSlot()) {
            (item.previous = tail).next = item;
            if (!dynamicSlots)
                dynamicSlots = true;
            return tail = item;
        }
        (item.next = head).previous = item;
        return head = item;
    }

    public boolean remove(ItemInfo info) {
        for (MenuItem item = head; item != null; item = item.next) {
            if (item.getInfo() == info) {
                MenuItem prev = item.previous, next = item.next;
                if (prev != null) prev.next = next;
                if (next != null) next.previous = prev;
                --size;
                item.removed = true;
                item.previous = null;
                if (item == head) head = next;
                if (item == tail) {
                    tail = prev;
                    if (prev != null && dynamicSlots && !prev.getInfo().hasDynamicSlot())
                        dynamicSlots = false;
                }
                menu.itemRemoved(item);
                return true;
            }
        }
        return false;
    }

    public boolean contains(ItemInfo info) {
        for (MenuItem item = head; item != null; item = item.next)
            if (!item.removed && item.getInfo() == info)
                return true;
        return false;
    }

    public void clear() {
        for (MenuItem item = head, next; item != null;) {
            next = item.next;
            item.next = item.previous = null;
            item = next;
        }
        head = tail = null;
        dynamicSlots = false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean hasDynamicSlots() {
        return dynamicSlots;
    }

    public MenuItem head() {
        return head;
    }

    public MenuItem tail() {
        return tail;
    }

    @Override
    public Iterator<MenuItem> iterator() {
        return new Iterator<MenuItem>() {
            MenuItem item = head, last;

            MenuItem getItem() {
                while (item != null && item.removed)
                    item = item.next; // Next retains when removed
                return item;
            }

            @Override
            public boolean hasNext() {
                return getItem() != null;
            }

            @Override
            public MenuItem next() {
                if (getItem() == null) throw new IllegalStateException();
                item = (last = item).next;
                return last;
            }

            @Override
            public void remove() {
                if (last == null)
                    throw new IllegalStateException();
                MenuGrid.this.remove(last.getInfo());
                last = null;
            }
        };
    }

    @Override
    public void forEach(Consumer<? super MenuItem> action) {
        for (MenuItem item = head; item != null; item = item.next)
            action.accept(item);
    }

    @Override
    public Spliterator<MenuItem> spliterator() {
        return Spliterators.spliterator(iterator(), size, Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.NONNULL);
    }
}
