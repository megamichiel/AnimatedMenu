package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AnimatedMenu extends AbstractMenu {
    
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
    private final MenuSettings settings = new MenuSettings();
    private final MenuType menuType;
    private final int titleUpdateDelay;
    private final IPlaceholder<String> permission, permissionMessage;

    private int titleUpdateTick = 0;
    
    private final Map<Player, Inventory> openMenu = new ConcurrentHashMap<>();
    
    public AnimatedMenu(AnimatedMenuPlugin plugin, String name, AnimatedText title,
                        int titleUpdateDelay, MenuType type,
                        StringBundle permission, StringBundle permissionMessage) {
        super(plugin, name, type.getSize());
        this.plugin = plugin;
        this.menuTitle = title;
        this.titleUpdateDelay = titleUpdateDelay;
        
        this.menuType = type;
        this.permission = permission == null ? null : permission.tryCache();
        this.permissionMessage = permissionMessage == null ?
                IPlaceholder.constant(ChatColor.RED + "You are not allows to open that menu!") : permissionMessage.tryCache();
    }

    public MenuSettings getSettings() {
        return settings;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public void handleMenuClose(Player who) {
        openMenu.remove(who);
        for (Consumer<? super Player> consumer : settings.getCloseListeners())
            consumer.accept(who);
    }

    @Override
    protected Iterator<Entry<Player, Inventory>> getViewers() {
        return openMenu.entrySet().iterator();
    }

    private Inventory createInventory(Player who) {
        String title = menuTitle.get().toString(who);
        if (title.length() > 32) title = title.substring(0, 32);
        Inventory inv;
        if (menuType.getInventoryType() == InventoryType.CHEST)
            inv = Bukkit.createInventory(null, menuType.getSize(), title);
        else inv = Bukkit.createInventory(null, menuType.getInventoryType(), title);
        ItemStack[] contents = new ItemStack[inv.getSize()];
        MenuItem[] items = this.items.get(who);
        if (items == null) this.items.put(who, items = new MenuItem[inv.getSize()]);
        for (int slot = 0; slot < menuGrid.getSize(); slot++) {
            MenuItem item = menuGrid.getItems()[slot];
            if (!item.getSettings().isHidden(plugin, who))
                contents[(items[slot] = item).getSlot(who, contents)] = item.load(nagger, who);
        }
        inv.setContents(contents);
        return inv;
    }
    
    public void open(Player who) {
        if (plugin == null) return;
        if (permission != null && !who.hasPermission(permission.invoke(plugin, who))) {
            if (permissionMessage != null) who.sendMessage(permissionMessage.invoke(plugin, who));
            return;
        }
        for (Consumer<? super Player> consumer : settings.getOpenListeners())
            consumer.accept(who);
        Inventory inv = createInventory(who);
        who.openInventory(inv);
        openMenu.put(who, inv);
    }
    
    public void tick() {
        if (plugin == null) return;
        if (menuTitle.size() > 1 && titleUpdateTick++ == titleUpdateDelay) {
            titleUpdateTick = 0;
            menuTitle.next();
            try {
                for (Player player : openMenu.keySet()) {
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
                nagger.nag("Unable to update menu title!");
                nagger.nag(ex);
            }
        }
        super.tick();
    }
}
