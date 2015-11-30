package me.megamichiel.animatedmenu.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.ToString;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Connection;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

@ToString
public class AnimatedMenu {
	
	@Getter
	private final String name;
	private final Inventory inv;
	@Getter
	private final MenuSettings settings;
	@Getter
	private final MenuGrid menuGrid;
	@Getter
	private final MenuType type;
	@Getter
	private final List<Connection> connections = new ArrayList<>();
	private final AtomicInteger currentTick = new AtomicInteger();
	
	public AnimatedMenu(String name, String title, MenuType type) {
		this.name = name;
		if(type.getInventoryType() == InventoryType.CHEST) 
			inv = Bukkit.createInventory(null, type.getSize(), title);
		else inv = Bukkit.createInventory(null, type.getInventoryType(), title);
		settings = new MenuSettings();
		menuGrid = new MenuGrid(inv, type.getSize());
		this.type = type;
	}
	
	public void open(Player who) {
		who.openInventory(inv);
		if(settings.getOpenSound() != null)
			who.playSound(who.getLocation(), settings.getOpenSound(), 1F, settings.getOpenSoundPitch());
	}
	
	public void load(ConfigurationSection cfg) {
		settings.load(cfg);
		ConfigurationSection items = cfg.getConfigurationSection("Items");
		if(items == null) {
			AnimatedMenuPlugin.getInstance().getLogger().warning("No items specified for " + name + "!");
			return;
		}
		for(String key : items.getKeys(false)) {
			ConfigurationSection itemSection = items.getConfigurationSection(key);
			MenuItem item = new MenuItem(MenuItemSettings.parse(this, key, itemSection));
			int slot = itemSection.getInt("Slot", -1);
			if(slot == -1) {
				AnimatedMenuPlugin.getInstance().getLogger().warning("No slot specified for item " + key + " in menu " + name + "!");
				continue;
			}
			menuGrid.setItem(slot - 1, item);
		}
	}
	
	public void tick() {
		for(MenuItem item : menuGrid.getItems())
			if(item != null)
				item.tick();
		if(currentTick.getAndIncrement() == settings.getConnectionDelay()) {
			currentTick.set(0);
			for(Connection con : connections) {
				con.connect();
			}
		}
	}
}
