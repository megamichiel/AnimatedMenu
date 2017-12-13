package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.IAnimatable;
import me.megamichiel.animationlib.bukkit.BukkitCommandAPI;
import me.megamichiel.animationlib.command.CommandInfo;
import me.megamichiel.animationlib.command.exec.CommandContext;
import me.megamichiel.animationlib.command.exec.CommandExecutor;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.LoggerNagger;
import me.megamichiel.animationlib.util.Subscription;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Menu extends AbstractMenu implements CommandExecutor {

    private static final MenuSession.Property<Menu> ORIGIN = MenuSession.Property.of();

    private final Plugin plugin;
    private final IAnimatable<? extends IPlaceholder<String>> title;
    private final boolean titleAnimatable;
    private final Settings settings = new Settings();
    private final Map<Player, Menu> navigation = new WeakHashMap<>();

    private Subscription[] command;
    
    public Menu(Plugin plugin, String name, IAnimatable<? extends IPlaceholder<String>> title, Type type) {
        super(plugin instanceof Nagger ? (Nagger) plugin : LoggerNagger.of(plugin.getLogger()), name, type);

        this.plugin = plugin;
        this.title = title;
        titleAnimatable = title.isAnimated();
    }

    public Menu(Plugin plugin, String name, String title, Type type) {
        this(plugin, name, IAnimatable.single(new StringBundle(null, title)), type);
    }

    public Menu(Plugin plugin, String title, Type type) {
        this(plugin, UUID.randomUUID().toString(), IAnimatable.single(new StringBundle(null, title)), type);
    }

    public boolean registerCommand(String plugin) {
        if (command == null) {
            CommandInfo openCommand = settings.getOpenCommand();
            if (openCommand != null) {
                BukkitCommandAPI api = BukkitCommandAPI.getInstance();
                List<Subscription> sub = new ArrayList<>();

                Command command = api.getCommand(openCommand.name());
                if (command != null) {
                    sub.addAll(api.deleteCommands((lbl, cmd) -> cmd == command && lbl.equals(openCommand.name())));
                }

                for (String alias : openCommand.aliases()) {
                    Command cmd = api.getCommand(alias);
                    if (cmd != null) {
                        sub.addAll(api.deleteCommands((lbl, $cmd) -> $cmd == cmd && lbl.equals(alias)));
                    }
                }
                // Make sure the command is removed first on unregister:
                sub.add(0, api.registerCommand(plugin, openCommand));

                this.command = sub.toArray(new Subscription[0]);
                return true;
            }
        }
        return false;
    }

    public boolean isCommandRegistered() {
        return command != null;
    }

    public boolean unregisterCommand() {
        Subscription[] cmd = command;
        if (cmd != null) {
            command = null;
            for (Subscription sub : cmd) {
                sub.unsubscribe();
            }
            return true;
        }
        return false;
    }

    public void closeAll() {
        closeAll(null);
    }

    public void closeAll(String reason) {
        forEachViewer(player -> {
            if (reason != null) {
                player.sendMessage(reason);
            }
            player.closeInventory();
        });
    }

    public Settings getSettings() {
        return settings;
    }

    public MenuSession handleMenuClose(Player player, MenuSession newSession) {
        MenuSession session = removeViewer(player);
        if (session != null) {
            for (BiConsumer<? super Player, ? super MenuSession> listener : settings.closeListeners) {
                listener.accept(player, session);
            }
            Menu origin = session.get(ORIGIN);
            if (origin != null) {
                if (newSession != null) {
                    newSession.set(ORIGIN, origin);
                } else {
                    origin.navigation.put(player, this);
                }
            }
        }
        return session;
    }

    private void openInventory(Player who, Consumer<? super MenuSession> action) {
        who.openInventory(setup(who, $session -> {
            Type type = getType();
            String title = this.title.get().invoke($session, who);
            if (title.length() > 32) {
                title = title.substring(0, 32);
            }
            return type.isChest() ? Bukkit.createInventory($session, type.getSize(), title) : Bukkit.createInventory($session, type.getInventoryType(), title);
        }, action).getInventory());
    }

    /**
     * Makes a given Player open this menu
     */
    public final void open(Player who) {
        open(who, null);
    }

    /**
     * Makes a given Player open this menu
     *
     * @param who The Player that should open the menu
     * @param ready A Consumer that will receive a MenuSession instance when the Player has opened the menu. If the player cannot open the menu, <i>null</i> is passed
     */
    public void open(Player who, Consumer<? super MenuSession> ready) {
        boolean saveNavigation = settings.saveNavigation();
        if (saveNavigation) {
            Menu menu = navigation.remove(who);
            if (menu != null && menu != this) {
                menu.open(who, session -> {
                    if (session != null) {
                        session.set(ORIGIN, this);
                    }
                    if (ready != null) {
                        ready.accept(session);
                    }
                });
                return;
            }
        }

        String s = settings.canOpen(who);
        if (s != null) {
            if (!s.isEmpty()) {
                who.sendMessage(s);
            }
            if (ready != null) {
                ready.accept(null);
            }
            return;
        }
        Consumer<MenuSession> action = session -> {
            if (settings.saveNavigation()) {
                session.set(ORIGIN, this);
            }
            Player player = session.getPlayer();
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory != null) {
                InventoryHolder holder = inventory.getHolder();
                if (holder instanceof MenuSession) {
                    AbstractMenu menu = ((MenuSession) holder).getMenu();
                    if (menu instanceof Menu) {
                        ((Menu) menu).handleMenuClose(who, session);
                    }
                }
            }
            InventoryHolder holder;
            if ((holder = player.getOpenInventory().getTopInventory().getHolder()) instanceof MenuSession) {
                ((Menu) ((MenuSession) holder).getMenu()).handleMenuClose(who, session);
            }
            for (BiConsumer<? super Player, ? super MenuSession> listener : settings.openListeners) {
                listener.accept(player, session);
            }
            if (ready != null) {
                ready.accept(session);
            }
        };
        List<Predicate<? super Player>> awaits = settings.getAwaits();
        if (awaits.isEmpty()) {
            openInventory(who, action);
        } else {
            Function<Player, String> message = settings.getWaitMessage();
            if (message != null) {
                who.sendMessage(message.apply(who));
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                for (Predicate<? super Player> await : awaits) {
                    if (!await.test(who)) {
                        return;
                    }
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> openInventory(who, action));
            });
        }
    }

    @Override
    public void tick() {
        if (titleAnimatable && title.tick()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> forEachSession(session -> {
                String title = this.title.get().invoke(session, session.getPlayer());
                session.setTitle(title.length() > 32 ? title.substring(0, 32) : title);
            }));
        }
        super.tick();
    }

    @Override
    public void onCommand(CommandContext ctx) {
        CommandSender sender = (CommandSender) ctx.getSender();

        String[] args = ctx.getArgs();
        CommandExecutor fallback = getSettings().getFallbackExecutor();
        if (sender instanceof Player) {
            if (fallback == null || args.length == 0) {
                open((Player) sender);
            } else {
                fallback.onCommand(ctx);
            }
        } else if (fallback != null) {
            fallback.onCommand(ctx);
        }
    }

    public String getDefaultUsage() {
        return "/<command>";
    }

    public static final class Settings {

        private final List<Function<? super Player, ? extends String>> openFilters = new ArrayList<>();
        private final List<BiConsumer<? super Player, ? super MenuSession>> openListeners = new ArrayList<>();
        private final List<BiConsumer<? super Player, ? super MenuSession>> closeListeners = new ArrayList<>();

        private final List<Predicate<? super Player>> awaits = new ArrayList<>();
        private Function<Player, String> waitMessage;

        private ItemStack opener;
        private boolean openerName, openerLore;
        private int openerJoinSlot = -1;

        private boolean openOnJoin;
        private CommandInfo openCommand;
        private CommandExecutor fallbackExecutor;
        private boolean hiddenFromCommand = false, saveNavigation = false;

        private Settings() { }

        public boolean shouldOpenOnJoin() {
            return openOnJoin;
        }

        public void setOpener(ItemStack opener, int slot) {
            this.opener = opener;
            this.openerJoinSlot = slot;
            if (opener == null || !opener.hasItemMeta()) {
                return;
            }
            ItemMeta meta = opener.getItemMeta();
            openerName = meta.hasDisplayName();
            openerLore = meta.hasLore();
        }

        public boolean hasOpener() {
            return opener != null;
        }

        public boolean giveOpener(PlayerInventory inv, boolean add) {
            if (opener != null) {
                if (add) {
                    inv.addItem(opener);
                } else if (openerJoinSlot >= 0) {
                    inv.setItem(openerJoinSlot, opener);
                }
                return true;
            }
            return false;
        }

        public boolean canOpenWith(ItemStack item, ItemMeta meta) {
            ItemStack opener = this.opener;
            if (opener == null || item.getType() != opener.getType() || item.getAmount() < opener.getAmount()
                    || (item.getDurability() != Short.MAX_VALUE && item.getDurability() != opener.getDurability())) {
                return false;
            }
            if (openerName || openerLore) {
                ItemMeta im = opener.getItemMeta();
                return (!openerName || im.getDisplayName().equals(meta.getDisplayName())) &&
                       (!openerLore || im.getLore().equals(meta.getLore()));
            }
            return true;
        }

        public void setOpenOnJoin(boolean openOnJoin) {
            this.openOnJoin = openOnJoin;
        }

        public void addListener(BiConsumer<? super Player, ? super MenuSession> listener, boolean close) {
            (close ? closeListeners : openListeners).add(listener);
        }

        public void addListener(Consumer<? super Player> listener, boolean close) {
            addListener((player, session) -> listener.accept(player), close);
        }

        public void setOpenCommand(Menu executor, String name, String usage, String description, String[] aliases) {
            if (name == null) {
                openCommand = null;
            } else {
                openCommand = new CommandInfo(name, executor).usage(usage).desc(description).aliases(aliases);
            }
        }

        public CommandInfo getOpenCommand() {
            return openCommand;
        }

        public void setHiddenFromCommand(boolean hiddenFromCommand) {
            this.hiddenFromCommand = hiddenFromCommand;
        }

        public boolean isHiddenFromCommand() {
            return hiddenFromCommand;
        }

        public CommandExecutor getFallbackExecutor() {
            return fallbackExecutor;
        }

        public void setFallbackExecutor(CommandExecutor fallbackExecutor) {
            this.fallbackExecutor = fallbackExecutor;
        }

        public void setSaveNavigation(boolean saveNavigation) {
            this.saveNavigation = saveNavigation;
        }

        public boolean saveNavigation() {
            return saveNavigation;
        }

        public void addOpenFilter(Function<? super Player, ? extends String> filter) {
            if (filter == null) {
                throw new NullPointerException("filter");
            }
            openFilters.add(filter);
        }

        public void removeOpenFilter(Function<? super Player, ? extends String> filter) {
            openFilters.remove(filter);
        }

        public String canOpen(Player player) {
            String str;
            for (Function<? super Player, ? extends String> filter : openFilters) {
                if ((str = filter.apply(player)) != null) {
                    return str;
                }
            }
            return null;
        }

        public List<Predicate<? super Player>> getAwaits() {
            return awaits;
        }

        public void setWaitMessage(Function<Player, String> waitMessage) {
            this.waitMessage = waitMessage;
        }

        public Function<Player, String> getWaitMessage() {
            return waitMessage;
        }
    }
}
