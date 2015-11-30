package me.megamichiel.animatedmenu.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuSettings {
	
	@Getter
	private ItemStack opener;
	private boolean openerName, openerLore;
	@Getter @Setter
	private int openerJoinSlot;
	@Getter @Setter
	private Sound openSound;
	@Getter @Setter
	private float openSoundPitch = 1F;
	@Getter @Setter
	private String openCommand;
	@Getter @Setter
	private int connectionDelay;
	
	public void load(ConfigurationSection section) {
		Logger logger = AnimatedMenuPlugin.getInstance().getLogger();
		if(section.isSet("Menu-Opener")) {
			opener = MenuItemSettings.parseItemStack(section.getString("Menu-Opener"));
			openerName = section.isSet("Menu-Opener-Name");
			openerLore = section.isSet("Menu-Opener-Lore");
			if(openerName || openerLore) {
				ItemMeta meta = opener.getItemMeta();
				if(openerName)
					meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("Menu-Opener-Name")));
				if(openerLore) {
					List<String> lore = new ArrayList<>();
					for(String line : section.getStringList("Menu-Opener-Lore"))
						lore.add(ChatColor.translateAlternateColorCodes('&', line));
				}
				opener.setItemMeta(meta);
			}
			openerJoinSlot = section.getInt("Menu-Opener-Slot", -1);
		}
		if(section.isSet("Open-Sound")) {
			try {
				openSound = Sound.valueOf(section.getString("Open-Sound").toUpperCase().replace('-', '_'));
				if(section.isSet("Open-Sound-Pitch"))
					openSoundPitch = Float.parseFloat(section.getString("Open-Sound-Pitch"));
			} catch (NumberFormatException ex) {
				logger.warning("Invalid pitch: " + section.getString("Open-Sound-Pitch"));
			} catch (Exception ex) {
				logger.warning("Couldn't find a sound for name " + section.getString("Open-Sound") + "!");
			}
		}
		openCommand = section.contains("Command") ? section.getString("Command").toLowerCase() : null;
		connectionDelay = section.getInt("Connection-Delay", 10 * 20);
		if(connectionDelay < 0) {
			logger.warning(connectionDelay + " is not a valid number!");
			connectionDelay = 10 * 20;
		}
	}
	
	public boolean hasOpenerName() {
		return openerName;
	}
	
	public boolean hasOpenerLore() {
		return openerLore;
	}
	
	public void setOpener(ItemStack opener) {
		this.opener = opener;
		ItemMeta meta = opener.getItemMeta();
		openerName = meta.hasDisplayName();
		openerLore = meta.hasLore();
	}
}
