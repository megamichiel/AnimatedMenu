package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.command.exec.CommandContext;
import me.megamichiel.animationlib.command.exec.CommandExecutor;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.db.SQLHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class AnimatedMenu extends AbstractMenu implements CommandExecutor {
    
    private static final Method GET_HANDLE, SEND_PACKET, UPDATE_INVENTORY;
    private static final Field PLAYER_CONNECTION, ACTIVE_CONTAINER, WINDOW_ID;
    private static final Constructor<?> OPEN_WINDOW, CHAT_MESSAGE;
    
    static {
        Method getHandle = null, sendPacket = null, updateInventory = null;
        Field playerConnection = null, activeContainer = null, windowId = null;
        Constructor<?> openWindow = null, chatMessage = null;
        try {
            Class<?> clazz = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftPlayer");
            getHandle = clazz.getMethod("getHandle");
            playerConnection = getHandle.getReturnType().getField("playerConnection");
            Class<?> packet = Class.forName(playerConnection.getType().getPackage().getName() + ".PacketPlayOutOpenWindow");
            for (Constructor<?> cnst : packet.getConstructors())
                if (cnst.getParameterTypes().length == 5)
                    openWindow = cnst;
            sendPacket = playerConnection.getType().getMethod("sendPacket", packet.getInterfaces()[0]);
            Class<?> msg = Class.forName(packet.getPackage().getName() + ".ChatMessage");
            chatMessage = msg.getConstructor(String.class, Object[].class);
            Class<?> container = Class.forName(packet.getPackage().getName() + ".Container");
            updateInventory = getHandle.getReturnType().getMethod("updateInventory", container);
            activeContainer = getHandle.getReturnType().getSuperclass().getField("activeContainer");
            windowId = activeContainer.getType().getField("windowId");
        } catch (Exception ex) {
            System.err.println("Something went wrong while loading NMS stuff!");
            ex.printStackTrace();
        }
        GET_HANDLE = getHandle;
        SEND_PACKET = sendPacket;
        PLAYER_CONNECTION = playerConnection;
        OPEN_WINDOW = openWindow;
        CHAT_MESSAGE = chatMessage;
        UPDATE_INVENTORY = updateInventory;
        ACTIVE_CONTAINER = activeContainer;
        WINDOW_ID = windowId;
    }

    private final AnimatedText menuTitle;
    private final MenuSettings settings = new MenuSettings(this);
    private final int titleUpdateDelay;
    private final IPlaceholder<String> permission, permissionMessage;
    private final List<SQLHandler.Entry> sqlAwaits = new ArrayList<>();
    private IPlaceholder<String> waitMessage;

    private int titleUpdateTick = 0;
    
    public AnimatedMenu(AnimatedMenuPlugin plugin, String name,
                        MenuLoader loader, AnimatedText title,
                        int titleUpdateDelay, MenuType type,
                        StringBundle permission, StringBundle permissionMessage) {
        super(plugin, name, type, loader);
        this.menuTitle = title;
        this.titleUpdateDelay = titleUpdateDelay;

        this.permission = permission == null ? null : permission.tryCache();
        this.permissionMessage = permissionMessage == null ?
                IPlaceholder.constant(ChatColor.RED + "You are not allowed to open that menu!") : permissionMessage.tryCache();
    }

    @Override
    public void dankLoad(AbstractConfig config) {
        super.dankLoad(config);
        List<String> list = config.getStringList("sql-await");
        if (list.isEmpty()) {
            String s = config.getString("sql-await");
            if (s != null) Collections.addAll(list, s.split(","));
        }
        if (!list.isEmpty()) {
            SQLHandler placeholders = SQLHandler.getInstance();
            list.stream().map(String::trim).map(placeholders::getEntry)
                    .filter(Objects::nonNull).forEach(sqlAwaits::add);
            StringBundle sb = StringBundle.parse(plugin, config.getString("wait-message"));
            waitMessage = sb == null ? null : sb.colorAmpersands().tryCache();
        }
    }

    public MenuSettings getSettings() {
        return settings;
    }

    public void handleMenuClose(Player who, AnimatedMenu newMenu) {
        if (removeViewer(who)) settings.callListeners(who, true);
    }

    private Inventory createInventory(Player who) {
        String title = menuTitle.get().toString(who);
        if (title.length() > 32) title = title.substring(0, 32);
        Inventory inv;
        if (menuType.getInventoryType() == InventoryType.CHEST)
            inv = Bukkit.createInventory(null, menuType.getSize(), title);
        else inv = Bukkit.createInventory(null, menuType.getInventoryType(), title);
        setup(who, inv);
        return inv;
    }

    public String canOpen(Player who) {
        if (permission != null && !who.hasPermission(permission.invoke(plugin, who))) {
            if (permissionMessage != null)
                return permissionMessage.invoke(plugin, who);
            return "";
        }
        return null;
    }
    
    public void open(Player who, Consumer<Inventory> ready) {
        if (!sqlAwaits.isEmpty()) {
            if (waitMessage != null)
                who.sendMessage(waitMessage.invoke(plugin, who));
            SQLHandler.getInstance().awaitRefresh(who, sqlAwaits, () -> {
                settings.callListeners(who, false);
                ready.accept(createInventory(who));
            });
            return;
        }
        settings.callListeners(who, false);
        ready.accept(createInventory(who));
    }

    @Override
    public void tick() {
        if (plugin == null) return;
        if (menuTitle.size() > 1 && titleUpdateTick++ == titleUpdateDelay) {
            titleUpdateTick = 0;
            menuTitle.next();
            try {
                for (Player player : getViewers()) {
                    String title = this.menuTitle.get().toString(player);
                    if (title.length() > 32) title = title.substring(0, 32);
                    Object handle = GET_HANDLE.invoke(player);
                    Object container = ACTIVE_CONTAINER.get(handle);
                    Object packet = OPEN_WINDOW.newInstance(WINDOW_ID.getInt(container), menuType.getNmsName(),
                            CHAT_MESSAGE.newInstance(title, new Object[0]), menuType.getSize(), 0);
                    SEND_PACKET.invoke(PLAYER_CONNECTION.get(handle), packet);
                    UPDATE_INVENTORY.invoke(handle, container);
                }
            } catch (Exception ex) {
                plugin.nag("Unable to update menu title!");
                plugin.nag(ex);
            }
        }
        super.tick();
    }

    @Override
    public void onCommand(CommandContext ctx) {
        CommandSender sender = (CommandSender) ctx.getSender();
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player for that!");
            return;
        }
        plugin.getMenuRegistry().openMenu((Player) sender, this);
    }

    protected String getDefaultUsage() {
        return "/<command>";
    }
}
