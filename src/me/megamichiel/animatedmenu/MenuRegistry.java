package me.megamichiel.animatedmenu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import lombok.Getter;
import me.megamichiel.animatedmenu.animation.AnimatedText;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.menu.MenuType;
import me.megamichiel.animatedmenu.util.StringBundle;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class MenuRegistry implements Iterable<AnimatedMenu>, Runnable {
	
	@Getter private final List<AnimatedMenu> menus = new ArrayList<>();
	private final AnimatedMenuPlugin plugin;
	@Getter
	private final Map<Player, AnimatedMenu> openMenu = new WeakHashMap<>();
	
	public MenuRegistry(AnimatedMenuPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void run() {
		for(AnimatedMenu menu : this) {
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
	public AnimatedMenu getOpenedMenu(Player who) {
		return openMenu.get(who);
	}
	
	/**
	 * Clears all menus
	 */
	public void clear() {
		menus.clear();
	}
	
	/**
	 * Adds a menu to the registry
	 * @param menu the menu to add
	 */
	public void add(AnimatedMenu menu) {
		menus.add(menu);
	}
	
	/**
	 * Removes a menu from the registry
	 * @param menu the menu to remove
	 */
	public void remove(AnimatedMenu menu) {
		menus.remove(menu);
	}
	
	/**
	 * Gets a menu by name
	 * @param name the name of the menu, case insensitive
	 * @return the menu by name, or null if none was found
	 */
	public AnimatedMenu getMenu(String name) {
		for(AnimatedMenu menu : this)
			if(menu.getName().equalsIgnoreCase(name))
				return menu;
		return null;
	}
	
	/**
	 * Get a new iterator of all menus
	 */
	@Override
	public Iterator<AnimatedMenu> iterator() {
		return menus.iterator();
	}
	
	/**
	 * Make a player open a menu
	 * @param who the player who should open the menu
	 * @param menu the menu to open
	 */
	public void openMenu(Player who, AnimatedMenu menu) {
		AnimatedMenu prev = openMenu.remove(who);
		if (prev != null)
		{
			prev.getOpenMenu().remove(who);
		}
		menu.open(who);
		openMenu.put(who, menu);
	}
	
	void loadMenus() {
		clear();
		
		Logger logger = plugin.getLogger();
		logger.info("Loading menus...");
		
		File menus = new File(plugin.getDataFolder(), "menus");
		if(!menus.exists()) {
			menus.mkdirs();
			for (String fileName : new String[] { "example", "shop", "exampleplus" })
			{
				try {
					InputStream yml = plugin.getResource("menus/" + fileName + ".yml");
					if (yml == null) continue;
					FileOutputStream ymlOut = new FileOutputStream(new File(menus, fileName + ".yml"));
					int i;
					while((i = yml.read()) != -1)
						ymlOut.write(i);
					yml.close();
					ymlOut.close();
				} catch (Exception ex) {
					plugin.nag("Failed to create default menu " + fileName + "!");
					plugin.nag(ex);
				}
			}
		}
		File images = new File(plugin.getDataFolder(), "images");
		if(!images.exists()) {
			images.mkdir();
		}
		for(File file : menus.listFiles()) {
			int index = file.getName().lastIndexOf('.');
			if(index == -1) continue;
			String extension = file.getName().substring(index + 1);
			String name = file.getName().substring(0, index);
			AnimatedMenu menu;
			if(extension.equalsIgnoreCase("yml")) {
				if (name.contains(" "))
				{
					plugin.getLogger().warning("Menu file '" + name + ".yml' contains spaces!");
					name = name.replace(" ", "_");
				}
				FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
				menu = loadMenu(name, cfg);
			} else continue;
			logger.info("Loaded " + file.getName());
			add(menu);
		}
		logger.info(this.menus.size() + " menu" + (this.menus.size() == 1 ? "" : "s") + " loaded");
	}
	
	/**
	 * Loads a menu from a ConfigurationSection
	 * @param name the name of the menu
	 * @param cfg the ConfigurationSection
	 * @return the created menu
	 */
	public AnimatedMenu loadMenu(String name, ConfigurationSection cfg) {
		AnimatedText title = new AnimatedText();
		title.load(plugin, cfg, "Menu-Name", new StringBundle(plugin, name));
		MenuType type;
		if (cfg.isSet("Menu-Type")) {
			type = MenuType.fromName(cfg.getString("Menu-Type").toUpperCase());
			if(type == null) {
				plugin.nag("Couldn't find a menu type by name " + cfg.getString("Menu-Type") + "!");
				type = MenuType.DISPENSER;
			}
		} else {
			type = MenuType.chest(cfg.getInt("Rows", 6));
		}
		AnimatedMenu menu = type.newMenu(plugin, name, title, cfg.getInt("Title-Update-Delay", 20));
		menu.load(plugin, cfg);
		return menu;
	}
}
