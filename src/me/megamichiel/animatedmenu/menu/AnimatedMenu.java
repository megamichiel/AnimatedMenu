package me.megamichiel.animatedmenu.menu;

import com.google.common.base.Predicate;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Supplier;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.AnimatedText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;

public class AnimatedMenu implements Supplier<AnimatedMenuPlugin> {
    
    private static final Method GET_HANDLE, SEND_PACKET, UPDATE_INVENTORY;
    private static final Field PLAYER_CONNECTION, ACTIVE_CONTAINER, WINDOW_ID;
    private static final Constructor<?> OPEN_WINDOW, CHAT_MESSAGE;
    
    static
    {
        Method getHandle = null, sendPacket = null, updateInventory = null;
        Field playerConnection = null, activeContainer = null, windowId = null;
        Constructor<?> openWindow = null, chatMessage = null;
        try
        {
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
        }
        catch (Exception ex) {
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

    private AnimatedMenuPlugin plugin;
    private final Nagger nagger;
    private final String name;
    private final AnimatedText menuTitle;
    private final MenuSettings settings = new MenuSettings();
    private final MenuType menuType;
    private final MenuGrid menuGrid;
    private final int titleUpdateDelay;
    private int titleUpdateTick = 0;
    
    private final Map<Player, Inventory> openMenu = new ConcurrentHashMap<>();
    
    public AnimatedMenu(Nagger nagger, String name, AnimatedText title, int titleUpdateDelay, MenuType type) {
        this.nagger = nagger;
        this.name = name;
        this.menuTitle = title;
        this.titleUpdateDelay = titleUpdateDelay;
        
        this.menuType = type;
        this.menuGrid = new MenuGrid(this, type.getSize());
    }

    public void init(AnimatedMenuPlugin plugin) {
        if (this.plugin == null)
            this.plugin = plugin;
    }

    public AnimatedMenuPlugin get() {
        return plugin;
    }

    public String getName() {
        return name;
    }

    public MenuSettings getSettings() {
        return settings;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public MenuGrid getMenuGrid() {
        return menuGrid;
    }

    public void handleMenuClose(Player who) {
        openMenu.remove(who);
        for (MenuItem item : menuGrid.getItems())
            if (item != null)
                item.handleMenuClose(who);
    }
    
    public Collection<? extends Player> getViewers()
    {
        return Collections.unmodifiableCollection(openMenu.keySet());
    }
    
    private Inventory createInventory(Player who)
    {
        String title = menuTitle.get().toString(who);
        if (title.length() > 32)
            title = title.substring(0, 32);
        Inventory inv;
        if (menuType.getInventoryType() == InventoryType.CHEST)
            inv = Bukkit.createInventory(null, menuType.getSize(), title);
        else inv = Bukkit.createInventory(null, menuType.getInventoryType(), title);
        for (int slot = 0; slot < menuType.getSize(); slot++)
        {
            MenuItem item = menuGrid.getItems()[slot];
            if (item != null && !item.getSettings().isHidden(plugin, who))
            {
                ItemStack i = item.load(nagger, who);
                inv.setItem(item.getSlot(this, who), i);
            }
        }
        return inv;
    }
    
    public void open(Player who) {
        if (plugin == null) return;
        for (Predicate<? super Player> predicate : settings.getOpenListeners())
            predicate.apply(who);
        Inventory inv = createInventory(who);
        who.openInventory(inv);
        openMenu.put(who, inv);
    }
    
    public void tick() {
        if (plugin == null) return;
        if (menuTitle.size() > 1 && titleUpdateTick++ == titleUpdateDelay)
        {
            titleUpdateTick = 0;
            menuTitle.next();
            try
            {
                for (Player player : openMenu.keySet())
                {
                    String title = this.menuTitle.get().toString(player);
                    if (title.length() > 32)
                        title = title.substring(0, 32);
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
        
        for (int index = 0; index < menuGrid.getSize(); index++) {
            MenuItem item = menuGrid.getItems()[index];
            if (item.tick()) {
                for (Entry<Player, Inventory> entry : openMenu.entrySet()) {
                    Player player = entry.getKey();
                    Inventory inv = entry.getValue();
                    int lastSlot = item.getLastSlot(player),
                            slot = item.getSlot(this, player);
                    boolean hidden = item.getSettings().isHidden(plugin, player);
                    ItemStack is = inv.getItem(slot);
                    if (hidden && is != null) {
                        inv.setItem(slot, null);
                        continue;
                    } else if (!hidden && is == null) {
                        inv.setItem(slot, item.load(nagger, player));
                        is = inv.getItem(slot);
                    }
                    if (!hidden) item.apply(nagger, player, is);
                }
            }
        }
    }
}
