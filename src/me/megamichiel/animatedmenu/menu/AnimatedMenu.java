package me.megamichiel.animatedmenu.menu;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedText;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AnimatedMenu {
	
	private final Nagger nagger;
	@Getter private final String name;
	@Getter private final AnimatedText menuTitle;
	@Getter private final MenuSettings settings;
	@Getter private final MenuType menuType;
	@Getter private final MenuGrid menuGrid;
	@Getter private final int titleUpdateDelay;
	private int titleUpdateTick = 0;
	
	private final Map<Player, Inventory> openMenu = new ConcurrentHashMap<Player, Inventory>();
	
	public AnimatedMenu(Nagger nagger, String name, AnimatedText title, int titleUpdateDelay, MenuType type) {
		this.nagger = nagger;
		this.name = name;
		settings = new MenuSettings();
		this.menuTitle = title;
		this.titleUpdateDelay = titleUpdateDelay;
		
		this.menuType = type;
		this.menuGrid = new MenuGrid(type.getSize());
	}
	
	public void handleMenuClose(HumanEntity who)
	{
		openMenu.remove(who);
	}
	
	public Collection<? extends Player> getViewers()
	{
		return Collections.unmodifiableCollection(openMenu.keySet());
	}
	
	private Inventory createInventory(Player who)
	{
		Inventory inv;
		if (menuType.getInventoryType() == InventoryType.CHEST)
			inv = Bukkit.createInventory(null, menuType.getSize(), menuTitle.get().toString(who));
		else inv = Bukkit.createInventory(null, menuType.getInventoryType(), menuTitle.get().toString(who));
		for (int slot = 0; slot < menuType.getSize(); slot++)
		{
			MenuItem item = menuGrid.getItem(slot);
			if (item != null && !item.getSettings().isHidden(who))
			{
				ItemStack i = item.load(nagger, who);
				inv.setItem(slot, i);
			}
		}
		return inv;
	}
	
	public void open(Player who) {
		Inventory inv = createInventory(who);
		who.openInventory(inv);
		if(settings.getOpenSound() != null)
			who.playSound(who.getLocation(), settings.getOpenSound(), 1F, settings.getOpenSoundPitch());
		openMenu.put(who, inv);
	}
	
	public void load(AnimatedMenuPlugin plugin, ConfigurationSection cfg) {
		settings.load(plugin, cfg);
		ConfigurationSection items = cfg.getConfigurationSection("Items");
		if(items == null) {
			plugin.nag("No items specified for " + name + "!");
			return;
		}
		for(String key : items.getKeys(false)) {
			ConfigurationSection itemSection = items.getConfigurationSection(key);
			MenuItem item = new MenuItem(new MenuItemSettings(key).load(plugin, getName(), itemSection));
			int slot = itemSection.getInt("Slot", -1);
			if(slot == -1) {
				plugin.nag("No slot specified for item " + key + " in menu " + name + "!");
				continue;
			}
			menuGrid.setItem(slot - 1, item);
		}
	}
	
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
		catch (Exception ex) {}
		GET_HANDLE = getHandle;
		SEND_PACKET = sendPacket;
		PLAYER_CONNECTION = playerConnection;
		OPEN_WINDOW = openWindow;
		CHAT_MESSAGE = chatMessage;
		UPDATE_INVENTORY = updateInventory;
		ACTIVE_CONTAINER = activeContainer;
		WINDOW_ID = windowId;
	}
	
	public void tick() {
		if (menuTitle.size() > 1 && titleUpdateTick++ == titleUpdateDelay)
		{
			titleUpdateTick = 0;
			menuTitle.next();
			try
			{
				for (Player player : openMenu.keySet())
				{
					String title = this.menuTitle.get().toString(player);
					Object handle = GET_HANDLE.invoke(player);
					Object container = ACTIVE_CONTAINER.get(handle);
					Object packet = OPEN_WINDOW.newInstance(WINDOW_ID.getInt(container), menuType.getNmsName(),
							CHAT_MESSAGE.newInstance(title, new Object[0]), menuType.getSize(), 0);
					SEND_PACKET.invoke(PLAYER_CONNECTION.get(handle), packet);
					UPDATE_INVENTORY.invoke(handle, container);
				}
			}
			catch (Exception ex) {}
		}
		
		for (int slot = 0; slot < menuType.getSize(); slot++)
		{
			MenuItem item = menuGrid.getItem(slot);
			if (item != null && item.tick())
			{
				for (Entry<Player, Inventory> entry : openMenu.entrySet())
				{
					boolean hidden = item.getSettings().isHidden(entry.getKey());
					ItemStack is = entry.getValue().getItem(slot);
					if (hidden && is != null)
					{
						entry.getValue().setItem(slot, null);
						continue;
					}
					else if (!hidden && is == null)
					{
						entry.getValue().setItem(slot, item.load(nagger, entry.getKey()));
						is = entry.getValue().getItem(slot);
					}
					if (!hidden) item.apply(nagger, entry.getKey(), is);
				}
			}
		}
	}
}
