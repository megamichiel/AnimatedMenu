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
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.menu.MenuType;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class MenuRegistry implements Iterable<AnimatedMenu>, Runnable {
	
	private final List<AnimatedMenu> menus = new ArrayList<>();
	private final AnimatedMenuPlugin plugin;
	@Getter
	private final Map<Player, AnimatedMenu> openMenu = new WeakHashMap<>();
	/*@Getter
	private final List<MenuItem> singleItems = new ArrayList<>();*/
	
	public MenuRegistry(AnimatedMenuPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void run() {
		for(AnimatedMenu menu : this) {
			try {
				menu.tick();
			} catch (Exception ex) {
				plugin.getLogger().warning("An error occured on ticking " + menu.getName() + ":");
				ex.printStackTrace();
			}
		}
		/*for(MenuItem item : singleItems) {
			try {
				item.tick();
			} catch (Exception ex) {
				plugin.getLogger().warning("An error occured on ticking item " + item.getSettings().getName() + ":");
				ex.printStackTrace();
			}
		}*/
	}
	
	public AnimatedMenu getOpenedMenu(Player who) {
		return openMenu.get(who);
	}
	
	public void clear() {
		menus.clear();
	}
	
	public void add(AnimatedMenu menu) {
		menus.add(menu);
	}
	
	public void remove(AnimatedMenu menu) {
		menus.remove(menu);
	}
	
	public AnimatedMenu getMenu(String name) {
		for(AnimatedMenu menu : this)
			if(menu.getName().equalsIgnoreCase(name))
				return menu;
		return null;
	}
	
	@Override
	public Iterator<AnimatedMenu> iterator() {
		return menus.iterator();
	}
	
	public void openMenu(Player who, AnimatedMenu menu) {
		menu.open(who);
		openMenu.put(who, menu);
	}
	
	public void loadMenus() {
		clear();
		
		Logger logger = plugin.getLogger();
		logger.info("Loading menus...");
		
		File menus = new File(plugin.getDataFolder(), "menus");
		if(!menus.exists()) {
			menus.mkdirs();
			try {
				InputStream yml = plugin.getResource("menus/example.yml");
				FileOutputStream ymlOut = new FileOutputStream(new File(menus, "example.yml"));
				int i;
				while((i = yml.read()) != -1)
					ymlOut.write(i);
				yml.close();
				ymlOut.close();
			} catch (Exception ex) {
				ex.printStackTrace();
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
				FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
				menu = loadMenu(name, cfg);
			} else continue;
			logger.info("Loaded " + file.getName());
			add(menu);
		}
		logger.info(this.menus.size() + " menu" + (this.menus.size() == 1 ? "" : "s") + " loaded");
	}
	
	public AnimatedMenu loadMenu(String name, ConfigurationSection cfg) {
		Logger logger = plugin.getLogger();
		String title = ChatColor.translateAlternateColorCodes('&', cfg.getString("Menu-Name", name));
		MenuType type;
		if(cfg.isSet("Menu-Type")) {
			type = MenuType.fromName(cfg.getString("Menu-Type").toUpperCase());
			if(type == null) {
				logger.warning("Couldn't find a menu type by name " + cfg.getString("Menu-Type") + "!");
				type = MenuType.DISPENSER;
			}
		} else {
			type = MenuType.chest(cfg.getInt("Rows", 6));
		}
		AnimatedMenu menu = type.newMenu(name, title);
		menu.load(cfg);
		return menu;
	}
}
