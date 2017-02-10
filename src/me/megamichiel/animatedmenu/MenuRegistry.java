package me.megamichiel.animatedmenu;

import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.menu.MenuLoader;
import me.megamichiel.animationlib.bukkit.BukkitCommandAPI;
import me.megamichiel.animationlib.command.CommandInfo;
import me.megamichiel.animationlib.command.CommandSubscription;
import me.megamichiel.animationlib.util.Subscription;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MenuRegistry implements Iterable<AnimatedMenu>, Runnable {

    private final MenuLoader menuLoader;

    private final Map<String, AnimatedMenu> menus = new HashMap<>();
    private final Map<AnimatedMenu, Runnable> commandSubscriptions = new HashMap<>();

    private final AnimatedMenuPlugin plugin;
    protected final Map<Player, AnimatedMenu> openMenu = new WeakHashMap<>();

    protected MenuRegistry(AnimatedMenuPlugin plugin, MenuLoader loader) {
        this.plugin = plugin;
        menuLoader = loader;
    }

    Map<Player, AnimatedMenu> getOpenMenu() {
        return openMenu;
    }

    Collection<AnimatedMenu> getMenus() {
        return Collections.unmodifiableCollection(menus.values());
    }

    void onDisable() {
        for (Object player : openMenu.keySet().toArray()) {
            ((Player) player).sendMessage(ChatColor.RED + "Menu closing due to plugin disabling/reloading");
            ((Player) player).closeInventory();
        }
        if (menuLoader != null) menuLoader.onDisable();
    }
    
    @Override
    public void run() {
        for (AnimatedMenu menu : menus.values()) {
            try {
                menu.tick();
            } catch (Exception ex) {
                plugin.nag("An error occured on ticking " + menu.getName() + ":");
                plugin.nag(ex);
            }
        }
    }
    
    /**
     * Get a player's opened menu
     * @param who the player to get the opened menu of
     * @return the player's opened menu, or null if the player doesn't have a menu open
     */
    AnimatedMenu getOpenedMenu(Player who) {
        return openMenu.get(who);
    }
    
    /**
     * Clears all menus
     */
    protected void clear() {
        menus.clear();
        commandSubscriptions.values().forEach(Runnable::run);
        commandSubscriptions.clear();
    }

    /**
     * Checks whether a menu is registered in this registry
     * @param menu the menu to check
     * @return true if the menu is registered, false otherwise
     */
    public boolean contains(AnimatedMenu menu) {
        return menus.get(menu.getName()) == menu;
    }
    
    /**
     * Adds a menu to the registry
     * @param menu the menu to add
     */
    public void add(AnimatedMenu menu) {
        menus.put(menu.getName(), menu);
        addMenuCommand(menu);
    }
    
    /**
     * Removes a menu from the registry
     * @param menu the menu to remove
     */
    public void remove(AnimatedMenu menu) {
        menus.remove(menu.getName());
        Optional.ofNullable(commandSubscriptions.remove(menu)).ifPresent(Runnable::run);
    }
    
    /**
     * Gets a menu by name
     * @param name the name of the menu, case insensitive
     * @return the menu by name, or null if none was found
     */
    public AnimatedMenu getMenu(String name) {
        return menus.get(name);
    }
    
    /**
     * Get a new iterator of all menus
     */
    @Override
    public Iterator<AnimatedMenu> iterator() {
        return menus.values().iterator();
    }

    @Override
    public void forEach(Consumer<? super AnimatedMenu> action) {
        menus.values().forEach(action);
    }

    @Override
    public Spliterator<AnimatedMenu> spliterator() {
        return menus.values().spliterator();
    }

    protected void closeMenu(Player who) {
        AnimatedMenu menu = openMenu.remove(who);
        if (menu != null) menu.handleMenuClose(who, null);
    }
    
    /**
     * Make a player open a menu
     * @param who the player who should open the menu
     * @param menu the menu to open
     */
    public void openMenu(Player who, AnimatedMenu menu) {
        String s = menu.canOpen(who);
        if (s != null) {
            if (!s.isEmpty()) who.sendMessage(s);
            return;
        }
        menu.open(who, inventory -> {
            AnimatedMenu old = openMenu.remove(who);
            if (old != null) old.handleMenuClose(who, menu);
            who.openInventory(inventory);
            openMenu.put(who, menu);
        });
    }
    
    void loadMenus() {
        clear();
        
        Logger logger = plugin.getLogger();
        logger.info("Loading menus...");

        File file = new File(plugin.getDataFolder(), "images");
        if (!file.exists() && !file.mkdir())
            plugin.getLogger().warning("Failed to create images folder!");

        List<AnimatedMenu> list = getMenuLoader().loadMenus();
        if (list != null) {
            list.forEach(this::add);
        }
        logger.info(this.menus.size() + " menu" + (this.menus.size() == 1 ? "" : "s") + " loaded");
    }

    private void addMenuCommand(AnimatedMenu menu) {
        CommandInfo openCommand = menu.getSettings().getOpenCommand();
        if (openCommand != null) {
            BukkitCommandAPI api = BukkitCommandAPI.getInstance();
            List<CommandSubscription<Command>> list = new ArrayList<>();
            deleteOld(api, openCommand.name(), list);
            for (String alias : openCommand.aliases())
                deleteOld(api, alias, list);
            // Make sure me command is removed first on disable:
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

    public MenuLoader getMenuLoader() {
        return menuLoader;
    }
}
