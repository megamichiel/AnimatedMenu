package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.menu.item.ItemInfo;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.bukkit.BukkitCommandAPI;
import me.megamichiel.animationlib.command.CommandInfo;
import me.megamichiel.animationlib.command.CommandSubscription;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.Subscription;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MenuRegistry implements Iterable<AnimatedMenu>, Runnable {

    private final Map<String, AnimatedMenu> menus = new ConcurrentHashMap<>();
    private final Map<AnimatedMenu, Runnable> commandSubscriptions = new ConcurrentHashMap<>();

    private final AnimatedMenuPlugin plugin;
    private final Map<Player, AnimatedMenu> openMenu = new WeakHashMap<>();

    public MenuRegistry(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    public Collection<AnimatedMenu> getMenus() {
        return Collections.unmodifiableCollection(menus.values());
    }

    public void onDisable() {
        menus.values().forEach(this::remove);
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
     * Returns the AnimatedMenuPlugin this registry belongs to
     */
    public AnimatedMenuPlugin getPlugin() {
        return plugin;
    }

    /**
     * Get a player's opened menu
     * @param who the player to get the opened menu of
     * @return the player's opened menu, or null if the player doesn't have a menu open
     */
    public AnimatedMenu getOpenedMenu(Player who) {
        return openMenu.get(who);
    }

    /**
     * Gets a player's opened menu session
     * @param who the player to get the session of
     * @return the player's opened menu session, or null if the player doesn't have a menu open
     */
    public MenuSession getSession(Player who) {
        AnimatedMenu menu = openMenu.get(who);
        return menu == null ? null : menu.getSession(who);
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
    protected boolean contains(AnimatedMenu menu) {
        return menus.get(menu.getName()) == menu;
    }

    public MenuBuilder newMenuBuilder(MenuType type) {
        return new MenuBuilder(type);
    }
    
    /**
     * Adds a menu to the registry
     * @param menu the menu to add
     */
    public void add(AnimatedMenu menu) {
        AnimatedMenu old = menus.put(menu.getName(), menu);
        if (old != null) {
            Optional.ofNullable(commandSubscriptions.remove(old)).ifPresent(Runnable::run);
        }
        addMenuCommand(menu);
    }
    
    /**
     * Removes a menu from the registry
     * @param menu the menu to remove
     */
    public void remove(AnimatedMenu menu) {
        menu.getViewers().forEach(player -> {
            player.sendMessage(ChatColor.RED + "Menu has been removed");
            player.closeInventory();
        });
        menus.remove(menu.getName(), menu);
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

    public void handleMenuClose(Player who) {
        AnimatedMenu menu = openMenu.remove(who);
        if (menu != null) menu.handleMenuClose(who, null);
    }

    void onOpen(Player who, AnimatedMenu menu, MenuSession session) {
        AnimatedMenu old = openMenu.put(who, menu);
        if (old != null) old.handleMenuClose(who, session);
    }

    private void addMenuCommand(AnimatedMenu menu) {
        CommandInfo openCommand = menu.getSettings().getOpenCommand();
        if (openCommand != null) {
            BukkitCommandAPI api = BukkitCommandAPI.getInstance();
            List<CommandSubscription<Command>> list = new ArrayList<>();
            deleteOld(api, openCommand.name(), list);
            for (String alias : openCommand.aliases())
                deleteOld(api, alias, list);
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

    public class MenuBuilder {

        private final MenuType type;

        private String name;
        private AnimatedText title;
        private int titleUpdatedDelay;
        private boolean hiddenFromCommand;
        private ItemInfo emptyItem;

        private final List<BiConsumer<? super Player, ? super MenuSession>> closeListeners = new ArrayList<>();
        private final List<ItemInfo> items = new ArrayList<>();

        MenuBuilder(MenuType type) {
            this.type = type;
        }

        public MenuBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public MenuBuilder withTitle(String title) {
            this.title = new AnimatedText(new StringBundle(null, title));
            return this;
        }

        public MenuBuilder withTitle(int updateDelay, String... titles) {
            title = new AnimatedText(Stream.of(titles).map(s -> new StringBundle(null, s)).toArray(StringBundle[]::new));
            titleUpdatedDelay = updateDelay;
            return this;
        }

        public MenuBuilder withTitle(int updateDelay, AnimatedText title) {
            this.title = title;
            titleUpdatedDelay = updateDelay;
            return this;
        }

        public MenuBuilder hiddenFromCommand() {
            hiddenFromCommand = true;
            return this;
        }

        public MenuBuilder withCloseListener(BiConsumer<? super Player, ? super MenuSession> listener) {
            closeListeners.add(listener);
            return this;
        }

        public MenuBuilder removeWhenClosed() {
            closeListeners.add((player, session) -> remove((AnimatedMenu) session.getMenu()));
            return this;
        }

        public MenuBuilder withItem(ItemInfo item) {
            items.add(item);
            return this;
        }

        public MenuBuilder withItem(ItemInfo... items) {
            Collections.addAll(this.items, items);
            return this;
        }

        public MenuBuilder withEmptyItem(ItemInfo item) {
            emptyItem = item;
            return this;
        }

        public AnimatedMenu build() {
            Validate.notNull(title, "Title cannot be null!");
            Validate.isTrue(!title.isEmpty(), "At least 1 title must be specified!");

            AnimatedMenu menu = new AnimatedMenu(name == null ? UUID.randomUUID().toString() : name, title, titleUpdatedDelay, type);

            MenuSettings settings = menu.getSettings();

            closeListeners.forEach(listener -> settings.addListener(listener, true));
            if (hiddenFromCommand) {
                settings.setHiddenFromCommand(true);
            }

            items.forEach(menu.getMenuGrid()::add);
            if (emptyItem != null) {
                menu.setEmptyItem(emptyItem);
            }

            add(menu);

            return menu;
        }

        public void openFor(Player player) {
            build().open(player);
        }
    }
}
