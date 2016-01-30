package me.megamichiel.animatedmenu.menu;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Nagger;
import me.megamichiel.animatedmenu.util.StringBundle;
import me.megamichiel.animatedmenu.util.StringUtil;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AnimatedMenu {
	@Getter
	private final String name;
	@Getter
	private final StringBundle title;
	@Getter
	private final MenuSettings settings;
	@Getter
	private final MenuType type;
	@Getter
	private final MenuGrid menuGrid;
	
	@Getter
	private final Map<Player, Inventory> openMenu = new ConcurrentHashMap<>();
	
	public AnimatedMenu(Nagger nagger, String name, String title, MenuType type) {
		this.name = name;
		settings = new MenuSettings();
		this.title = StringUtil.parseBundle(nagger, title);
		this.type = type;
		this.menuGrid = new MenuGrid(type.getSize());
	}
	
	private Inventory createInventory(Player who)
	{
		Inventory inv;
		if(type.getInventoryType() == InventoryType.CHEST) 
			inv = Bukkit.createInventory(null, type.getSize(), title.toString(who));
		else inv = Bukkit.createInventory(null, type.getInventoryType(), title.toString(who));
		for (int slot = 0; slot < type.getSize(); slot++)
		{
			MenuItem item = menuGrid.getItem(slot);
			if (item != null && !item.getSettings().isHidden(who))
			{
				ItemStack i = item.load(who);
				inv.setItem(slot, i);
			}
		}
		return inv;
	}
	
	public Inventory open(Player who) {
		Inventory inv = createInventory(who);
		who.openInventory(inv);
		if(settings.getOpenSound() != null)
			who.playSound(who.getLocation(), settings.getOpenSound(), 1F, settings.getOpenSoundPitch());
		openMenu.put(who, inv);
		return inv;
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
			MenuItem item = new MenuItem(MenuItemSettings.parse(plugin, this, key, itemSection));
			int slot = itemSection.getInt("Slot", -1);
			if(slot == -1) {
				plugin.nag("No slot specified for item " + key + " in menu " + name + "!");
				continue;
			}
			menuGrid.setItem(slot - 1, item);
		}
	}
	
	public void tick() {
		for (int slot = 0; slot < type.getSize(); slot++)
		{
			MenuItem item = menuGrid.getItem(slot);
			if (item != null)
			{
				if (item.tick())
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
							entry.getValue().setItem(slot, item.load(entry.getKey()));
							is = entry.getValue().getItem(slot);
						}
						if (!hidden) item.apply(entry.getKey(), is);
					}
				}
			}
		}
	}
}
