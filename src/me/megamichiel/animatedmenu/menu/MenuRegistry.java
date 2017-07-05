package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.bukkit.BukkitCommandAPI;
import me.megamichiel.animationlib.command.CommandInfo;
import me.megamichiel.animationlib.command.CommandSubscription;
import me.megamichiel.animationlib.util.Subscription;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MenuRegistry implements Iterable<AbstractMenu>, Runnable {

    private final Map<String, AbstractMenu> menus = new ConcurrentHashMap<>();
    private final Map<Menu, Runnable> commandSubscriptions = new ConcurrentHashMap<>();

    private final AnimatedMenuPlugin plugin;
    private final Map<Player, MenuSession> sessions = new WeakHashMap<>();

    public MenuRegistry(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    public void onDisable() {
        menus.values().forEach(this::remove);
    }
    
    @Override
    public void run() {
        for (AbstractMenu menu : menus.values()) {
            try {
                menu.tick();
            } catch (Exception ex) {
                plugin.nag("An error occured on ticking " + menu.getName() + ":");
                plugin.nag(ex);
            }
        }
    }

    /**
     * Returns the AnimatedMenuPlugin this registry belongs to
     */
    public AnimatedMenuPlugin getPlugin() {
        return plugin;
    }

    public Collection<AbstractMenu> getMenus() {
        return Collections.unmodifiableCollection(menus.values());
    }

    /**
     * Get a player's opened menu
     * @param who the player to get the opened menu of
     * @return the player's opened menu, or null if the player doesn't have a menu open
     */
    public Menu getOpenedMenu(Player who) {
        MenuSession session = sessions.get(who);
        return session == null ? null : (Menu) session.getMenu();
    }

    /**
     * Gets a player's opened menu session
     * @param who the player to get the session of
     * @return the player's opened menu session, or null if the player doesn't have a menu open
     */
    public MenuSession getSession(Player who) {
        return sessions.get(who);
    }
    
    /**
     * Clears all menus
     */
    public void clear() {
        menus.clear();
        commandSubscriptions.values().forEach(Runnable::run);
        commandSubscriptions.clear();
    }

    /**
     * Checks whether a menu is registered in this registry
     * @param menu the menu to check
     * @return true if the menu is registered, false otherwise
     */
    protected boolean contains(AbstractMenu menu) {
        return menus.get(menu.getName()) == menu;
    }
    
    /**
     * Adds a menu to the registry
     * @param menu the menu to add
     */
    public void add(AbstractMenu menu) {
        AbstractMenu old = menus.put(menu.getName(), menu);
        if (menu != old) {
            if (old instanceof Menu) {
                Optional.ofNullable(commandSubscriptions.remove(old)).ifPresent(Runnable::run);
            }
            if (menu instanceof Menu) {
                addMenuCommand((Menu) menu);
            }
        }
    }
    
    /**
     * Removes a menu from the registry
     * @param menu the menu to remove
     */
    public void remove(AbstractMenu menu) {
        menu.forEachViewer(player -> {
            player.sendMessage(ChatColor.RED + "Menu has been removed");
            player.closeInventory();
        });
        menus.remove(menu.getName(), menu);
        if (menu instanceof Menu) {
            Runnable task = commandSubscriptions.remove(menu);
            if (task != null) {
                task.run();
            }
        }
    }
    
    /**
     * Gets a menu by name
     * @param name the name of the menu, case insensitive
     * @return the menu by name, or null if none was found
     */
    public AbstractMenu getMenu(String name) {
        return menus.get(name);
    }
    
    /**
     * Get a new iterator of all menus
     */
    @Override
    public Iterator<AbstractMenu> iterator() {
        return menus.values().iterator();
    }

    @Override
    public void forEach(Consumer<? super AbstractMenu> action) {
        menus.values().forEach(action);
    }

    @Override
    public Spliterator<AbstractMenu> spliterator() {
        return menus.values().spliterator();
    }

    public void handleMenuClose(Player who) {
        MenuSession session = sessions.remove(who);
        if (session != null) {
            ((Menu) session.getMenu()).handleMenuClose(who, null);
        }
    }

    void onOpen(Player who, Menu menu, MenuSession session) {
        menus.putIfAbsent(menu.getName(), menu);
        MenuSession old = sessions.put(who, session);
        if (old != null) {
            ((Menu) old.getMenu()).handleMenuClose(who, session);
        }
    }

    private void addMenuCommand(Menu menu) {
        CommandInfo openCommand = menu.getSettings().getOpenCommand();
        if (openCommand != null) {
            BukkitCommandAPI api = BukkitCommandAPI.getInstance();
            List<CommandSubscription<Command>> list = new ArrayList<>();
            deleteOld(api, openCommand.name(), list);
            for (String alias : openCommand.aliases()) {
                deleteOld(api, alias, list);
            }
            // Make sure the command is removed first on disable:
            list.add(0, api.registerCommand(plugin, openCommand));
            commandSubscriptions.put(menu, createSubscription(list));
        }
    }

    private void deleteOld(BukkitCommandAPI api, String label, List<CommandSubscription<Command>> list) {
        Command command = api.getCommand(label);
        if (command != null) {
            list.addAll(api.deleteCommands((lbl, cmd) -> cmd == command && lbl.equals(label)));
        }
    }

    private Runnable createSubscription(List<CommandSubscription<Command>> list) {
        return () -> list.forEach(Subscription::unsubscribe);
    }
}
