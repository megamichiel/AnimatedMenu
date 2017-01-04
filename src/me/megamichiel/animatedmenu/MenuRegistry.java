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

    private MenuLoader menuLoader;

    private final List<AnimatedMenu> menus = new ArrayList<>();
    private final Map<AnimatedMenu, CommandSubscription<Command>> commandSubscriptions = new HashMap<>();

    private final AnimatedMenuPlugin plugin;
    private final Map<Player, AnimatedMenu> openMenu = new WeakHashMap<>();

    MenuRegistry(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    Map<Player, AnimatedMenu> getOpenMenu() {
        return openMenu;
    }

    List<AnimatedMenu> getMenus() {
        return menus;
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
        for (AnimatedMenu menu : menus) {
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
    private void clear() {
        menus.clear();
    }
    
    /**
     * Adds a menu to the registry
     * @param menu the menu to add
     */
    public void add(AnimatedMenu menu) {
        menus.add(menu);
        menu.init();
        addMenuCommand(menu);
    }
    
    /**
     * Removes a menu from the registry
     * @param menu the menu to remove
     */
    public void remove(AnimatedMenu menu) {
        menus.remove(menu);
        Optional.ofNullable(commandSubscriptions.remove(menu)).ifPresent(Subscription::unsubscribe);
    }
    
    /**
     * Gets a menu by name
     * @param name the name of the menu, case insensitive
     * @return the menu by name, or null if none was found
     */
    public AnimatedMenu getMenu(String name) {
        return menus.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findAny().orElse(null);
    }
    
    /**
     * Get a new iterator of all menus
     */
    @Override
    public Iterator<AnimatedMenu> iterator() {
        return menus.iterator();
    }

    @Override
    public void forEach(Consumer<? super AnimatedMenu> action) {
        menus.forEach(action);
    }

    @Override
    public Spliterator<AnimatedMenu> spliterator() {
        return menus.spliterator();
    }

    private boolean isRedirecting = false;

    public void closeMenu(Player who) {
        if (!isRedirecting) {
            AnimatedMenu menu = openMenu.remove(who);
            if (menu != null) menu.handleMenuClose(who, null);
        }
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
            isRedirecting = true;
            who.openInventory(inventory);
            isRedirecting = false;
            AnimatedMenu prev = openMenu.remove(who);
            if (prev != null) prev.handleMenuClose(who, menu);
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
            for (AnimatedMenu menu : list) {
                menu.init();
                addMenuCommand(menu);
            }
            menus.addAll(list);
        }
        logger.info(this.menus.size() + " menu" + (this.menus.size() == 1 ? "" : "s") + " loaded");
    }

    private void addMenuCommand(AnimatedMenu menu) {
        CommandInfo openCommand = menu.getSettings().getOpenCommand();
        if (openCommand != null) commandSubscriptions.put(menu,
                BukkitCommandAPI.getInstance().registerCommand(plugin, openCommand));
    }

    public MenuLoader getMenuLoader() {
        if (menuLoader == null)
            return menuLoader = plugin.getDefaultMenuLoader();
        return menuLoader;
    }
}
