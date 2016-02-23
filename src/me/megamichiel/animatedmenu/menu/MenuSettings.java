package me.megamichiel.animatedmenu.menu;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.StringUtil;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuSettings {
	
	@Getter
	private ItemStack opener;
	private boolean openerName, openerLore, openOnJoin;
	@Getter @Setter
	private int openerJoinSlot;
	@Getter @Setter
	private Sound openSound;
	@Getter @Setter
	private float openSoundPitch = 1F;
	@Getter @Setter
	private String openCommand;
	
	public void load(AnimatedMenuPlugin plugin, ConfigurationSection section) {
		if (section.isSet("Menu-Opener")) {
			opener = MenuItemSettings.parseItemStack(plugin, section.getString("Menu-Opener"));
			openerName = section.isSet("Menu-Opener-Name");
			openerLore = section.isSet("Menu-Opener-Lore");
			if(openerName || openerLore) {
				ItemMeta meta = opener.getItemMeta();
				if(openerName)
					meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("Menu-Opener-Name")));
				if(openerLore) {
					List<String> lore = new ArrayList<String>();
					for(String line : section.getStringList("Menu-Opener-Lore"))
						lore.add(ChatColor.translateAlternateColorCodes('&', line));
				}
				opener.setItemMeta(meta);
			}
			openerJoinSlot = section.getInt("Menu-Opener-Slot", -1);
		}
		openOnJoin = StringUtil.parseBoolean(section, "Open-On-Join", false);
		if (section.isSet("Open-Sound")) {
			try {
				openSound = Sound.valueOf(section.getString("Open-Sound").toUpperCase().replace('-', '_'));
				if(section.isSet("Open-Sound-Pitch"))
					openSoundPitch = Float.parseFloat(section.getString("Open-Sound-Pitch"));
			} catch (NumberFormatException ex) {
				plugin.nag("Invalid pitch: " + section.getString("Open-Sound-Pitch"));
			} catch (Exception ex) {
				plugin.nag("Couldn't find a sound for name " + section.getString("Open-Sound") + "!");
			}
		}
		openCommand = section.contains("Command") ? section.getString("Command").toLowerCase() : null;
	}
	
	public boolean shouldOpenOnJoin()
	{
		return openOnJoin;
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
