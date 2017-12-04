package me.megamichiel.animatedmenu.menu;

import org.bukkit.ChatColor;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public interface IMenuProvider<M extends AbstractMenu> extends Iterable<M> {

    static <M extends AbstractMenu> IMenuProvider<M> of(M menu) {
        return () -> new Iterator<M>() {
            boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public M next() {
                if (hasNext) {
                    hasNext = false;
                    return menu;
                }
                throw new NoSuchElementException();
            }
        };
    }

    /**
     * Finds a menu by name
     * @param name the name of the menu, case sensitive
     * @return the menu by name, or null if none was found
     */
    default M getMenu(String name) {
        for (M menu : this) {
            if (menu.getName().equals(name)) {
                return menu;
            }
        }
        return null;
    }

    class SimpleMenuProvider implements IMenuProvider<AbstractMenu> {

        private final String plugin;
        protected final Map<String, AbstractMenu> menus = new ConcurrentHashMap<>();

        public SimpleMenuProvider(String plugin) {
            this.plugin = plugin;
        }

        public AbstractMenu add(AbstractMenu menu) {
            AbstractMenu old = menus.put(menu.getName(), menu);
            if (old instanceof Menu) {
                ((Menu) old).closeAll(ChatColor.RED + "Menu reloaded");
                ((Menu) old).unregisterCommand();
            }
            if (menu instanceof Menu) {
                ((Menu) menu).registerCommand(plugin);
            }
            return old;
        }

        public boolean remove(AbstractMenu menu) {
            if (menus.remove(menu.getName(), menu)) {
                if (menu instanceof Menu) {
                    ((Menu) menu).closeAll(ChatColor.RED + "Menu removed");
                    ((Menu) menu).unregisterCommand();
                }
                return true;
            }
            return false;
        }

        public void clear() {
            for (AbstractMenu menu : menus.values()) {
                if (menu instanceof Menu) {
                    ((Menu) menu).closeAll(ChatColor.RED + "Menu removed");
                    ((Menu) menu).unregisterCommand();
                }
            }
            menus.clear();
        }

        @Override
        public Iterator<AbstractMenu> iterator() {
            return menus.values().iterator();
        }

        @Override
        public void forEach(Consumer<? super AbstractMenu> action) {
            menus.values().forEach(action);
        }

        @Override
        public AbstractMenu getMenu(String name) {
            return menus.get(name);
        }
    }
}
