package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class MenuRegistry implements Iterable<AbstractMenu>, Runnable {

    private final AnimatedMenuPlugin plugin;
    private final Set<IMenuProvider<?>> providers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public MenuRegistry(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        for (IMenuProvider<?> provider : providers) {
            for (AbstractMenu menu : provider) {
                try {
                    menu.tick();
                } catch (Exception ex) {
                    plugin.nag("An error occured on ticking " + menu.getName() + ":");
                    plugin.nag(ex);
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
