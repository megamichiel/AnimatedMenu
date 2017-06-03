package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.command.exec.CommandContext;
import me.megamichiel.animationlib.command.exec.CommandExecutor;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.pipeline.PipelineContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AnimatedMenu extends AbstractMenu implements CommandExecutor {

    private static final MenuSession.Property<AnimatedMenu> ORIGIN = MenuSession.Property.of();

    private final AnimatedText title;
    private final boolean titleAnimatable;
    private final MenuSettings settings = new MenuSettings(this);
    private final Map<Player, AnimatedMenu> navigation = new WeakHashMap<>();
    private final int titleUpdateDelay;

    private int titleUpdateTick;
    
    public AnimatedMenu(String name, AnimatedText title,
                        int titleUpdateDelay, MenuType type) {
        super(name, type);
        this.title = title;
        this.titleUpdateDelay = titleUpdateTick = titleUpdateDelay;
        titleAnimatable = title.isAnimated() || title.get(0).containsPlaceholders();
    }

    public AnimatedMenu(String name, String title, MenuType type) {
        this(name, new AnimatedText(new StringBundle(null, title)), 0, type);
    }

    public AnimatedMenu(String title, MenuType type) {
        this(UUID.randomUUID().toString(), new AnimatedText(new StringBundle(null, title)), 0, type);
    }

    public MenuSettings getSettings() {
        return settings;
    }

    public void requestTitleUpdate() {
        titleUpdateTick = 0;
    }

    public MenuSession handleMenuClose(Player who, MenuSession newSession) {
        MenuSession session = removeViewer(who);
        if (session != null) {
            settings.callListeners(who, session, true);
            AnimatedMenu origin = session.get(ORIGIN);
            if (origin != null) {
                if (newSession != null) {
                    newSession.set(ORIGIN, origin);
                } else {
                    origin.navigation.put(who, this);
                }
            }
        }
        return session;
    }

    private void openInventory(Player who, Consumer<? super MenuSession> action) {
        String title = this.title.get().toString(who);
        if (title.length() > 32) title = title.substring(0, 32);
        MenuType menuType = getMenuType();
        Inventory inv;
        if (menuType.getInventoryType() == InventoryType.CHEST) {
            inv = Bukkit.createInventory(null, menuType.getSize(), title);
        } else inv = Bukkit.createInventory(null, menuType.getInventoryType(), title);
        who.openInventory(inv);
        setup(who, inv, action);
    }

    public String canOpen(Player who) {
        IPlaceholder<String> permission = settings.getPermission();
        if (permission != null && !who.hasPermission(permission.invoke(plugin, who))) {
            if (settings.getPermissionMessage() != null) {
                return settings.getPermissionMessage().invoke(plugin, who);
            }
            return "";
        }
        return null;
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
            AnimatedMenu menu = navigation.get(who);
            if (menu != null && menu != this) {
                menu.open(who, session -> {
                    if (ready != null) {
                        ready.accept(session);
                    }
                    if (session != null) {
                        session.set(ORIGIN, this);
                    }
                });
                return;
            }
        }

        String s = canOpen(who);
        if (s != null) {
            if (!s.isEmpty()) {
                who.sendMessage(s);
            }
            if (ready != null) {
                ready.accept(null);
            }
            return;
        }
        List<Predicate<? super Player>> awaits = settings.getAwaits();
        Consumer<MenuSession> action = session -> {
            if (settings.saveNavigation()) {
                session.set(ORIGIN, this);
            }
            Player player = session.getPlayer();
            plugin.getMenuRegistry().onOpen(player, this, session);
            settings.callListeners(player, session, false);
            if (ready != null) {
                ready.accept(session);
            }
        };
        if (awaits.isEmpty()) {
            openInventory(who, action);
        } else {
            if (settings.getWaitMessage() != null) {
                who.sendMessage(settings.getWaitMessage().invoke(plugin, who));
            }
            plugin.post(() -> {
                for (Predicate<? super Player> await : awaits) {
                    if (!await.test(who)) {
                        return;
                    }
                }
                plugin.post(() -> openInventory(who, action), PipelineContext.SYNC);
            }, PipelineContext.ASYNC);
        }
    }

    @Override
    public void tick() {
        if (titleAnimatable && titleUpdateTick-- == 0) {
            title.next();
            if ((titleUpdateTick = titleUpdateDelay) >= 0) {
                plugin.post(() -> forEachSession(session -> {
                    String title = this.title.get().toString(session.getPlayer());
                    session.setTitle(title.length() > 32 ? title.substring(0, 32) : title);
                }), PipelineContext.SYNC);
            }
        }
        super.tick();
    }

    @Override
    public void onCommand(CommandContext ctx) {
        CommandSender sender = (CommandSender) ctx.getSender();

        String fallback = getSettings().getFallbackCommand();
        if (fallback != null && ctx.getArgs().length > 0) {
            if (fallback.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Too many arguments!");
            } else {
                StringBuilder sb = new StringBuilder(fallback);
                for (String arg : ctx.getArgs()) {
                    sb.append(' ').append(arg);
                }
                ((Player) sender).performCommand(sb.toString());
            }
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player for that!");
            return;
        }
        open((Player) sender);
    }

    public String getDefaultUsage() {
        return "/<command>";
    }
}
