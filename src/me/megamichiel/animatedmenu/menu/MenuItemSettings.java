package me.megamichiel.animatedmenu.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore;
import me.megamichiel.animatedmenu.animation.AnimatedMaterial;
import me.megamichiel.animatedmenu.animation.AnimatedName;
import me.megamichiel.animatedmenu.util.MaterialMatcher;
import me.megamichiel.animatedmenu.util.StringBundle;
import me.megamichiel.animatedmenu.util.StringUtil;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor @Getter @Setter @ToString
public class MenuItemSettings {
	
	private final String name;
	private AnimatedMaterial material;
	private AnimatedName displayName;
	private AnimatedLore lore;
	private int frameDelay;
	private Map<Enchantment, Integer> enchantments;
	private ItemClickListener clickListener;
	private String permission;
	private boolean hiddenWithoutPermission;
	private Color leatherArmorColor;
	private String skullOwner;
	
	public MenuItemSettings(String name) {
		this.name = name;
		material = new AnimatedMaterial();
		displayName = new AnimatedName();
		lore = new AnimatedLore();
		enchantments = new HashMap<Enchantment, Integer>();
		frameDelay = 20;
		hiddenWithoutPermission = false;
	}
	
	@SuppressWarnings("deprecation")
	public static MenuItemSettings parse(AnimatedMenu menu, String itemName, ConfigurationSection section) {
		AnimatedMenuPlugin plugin = AnimatedMenuPlugin.getInstance();
		
		AnimatedMaterial material = new AnimatedMaterial();
		if(section.getBoolean("Change-Material")) {
			material.load(menu, section.getConfigurationSection("Material"));
		} else {
			if(!section.contains("Material")) {
				plugin.getLogger().warning("Item " + itemName + " doesn't contain Material!");
				material.add(new ItemStack(Material.STONE));
			} else {
				material.add(parseItemStack(section.getString("Material")));
			}
		}
		AnimatedName name = new AnimatedName();
		if(section.getBoolean("Change-Name")) {
			name.load(menu, section.getConfigurationSection("Name"));
		} else {
			if(!section.contains("Name")) {
				plugin.getLogger().warning("Item " + itemName + " doesn't contain Name!");
				name.add(new StringBundle());
			} else {
				name.add(StringUtil.parseBundle(section.getString("Name")).colorAmpersands().loadPlaceHolders(menu));
			}
		}
		AnimatedLore lore = new AnimatedLore();
		lore.load(menu, section.getConfigurationSection("Lore"));
		Map<Enchantment, Integer> enchantments = new HashMap<>();
		for(String str : section.getStringList("Enchantments")) {
			String[] split = str.split(":");
			Enchantment ench;
			try {
				ench = Enchantment.getById(Integer.parseInt(split[0]));
			} catch (Exception ex) {
				plugin.getLogger().warning("Invalid enchantment id in " + str + "!");
				continue;
			}
			int level = 1;
			try {
				if(split.length == 2)
					level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				plugin.getLogger().warning("Invalid enchantment level in " + str + "!");
			}
			enchantments.put(ench, level);
		}
		ItemClickListener clickListener = new DefaultClickListener(section);
		boolean hide = section.getBoolean("Hide");
		Color color = getColor(section.getString("Color"));
		String owner = section.getString("SkullOwner");
		return new MenuItemSettings(itemName, material, name, lore,
				section.getInt("Frame-Delay", 20), enchantments, clickListener,
				section.getString("Permission"), hide, color, owner);
	}
	
	private static final Pattern COLOR_PATTERN = Pattern.compile("([0-9]+),\\s*([0-9]+),\\s*([0-9]+)");
	
	private static Color getColor(String val) {
		if(val == null) return Bukkit.getItemFactory().getDefaultLeatherColor();
		Matcher matcher = COLOR_PATTERN.matcher(val);
		if(matcher.matches()) {
			return Color.fromRGB(Integer.parseInt(matcher.group(1)),
					Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
		} else {
			try {
				return Color.fromRGB(Integer.parseInt(val, 16));
			} catch (NumberFormatException ex) {
				return Bukkit.getItemFactory().getDefaultLeatherColor();
			}
		}
	}
	
	static ItemStack parseItemStack(String str) {
		String[] split = str.split(":");
		MaterialMatcher matcher = MaterialMatcher.matcher(split[0]);
		if(!matcher.matches()) {
			System.err.println("Couldn't find appropiate material for " + split[0] + "! Defaulting to stone");
		}
		ItemStack item = new ItemStack(matcher.get());
		if(split.length > 1) {
			try {
				item.setAmount(Integer.parseInt(split[1]));
			} catch (NumberFormatException ex) {
				System.err.println("Invalid amount in " + str + "! Defaulting to 1");
			}
			if(split.length > 2) {
				try {
					item.setDurability(Short.parseShort(split[2]));
				} catch (NumberFormatException ex) {
					System.err.println("Invalid data value in " + str + "! Defaulting to 0");
				}
			}
		}
		return item;
	}
	
	public static Builder builder(String name) {
		return new Builder(name);
	}
	
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Builder {
		
		private final String name;
		private AnimatedMaterial material;
		private AnimatedName displayName;
		private AnimatedLore lore;
		private int frameDelay;
		private Map<Enchantment, Integer> enchantments;
		private ItemClickListener clickListener;
		private String permission;
		private boolean hiddenWithoutPermission;
		private Color leatherArmorColor;
		private String skullOwner;
		
		public Builder material(AnimatedMaterial material) {
			this.material = material;
			return this;
		}
		
		public Builder displayName(AnimatedName displayName) {
			this.displayName = displayName;
			return this;
		}
		
		public Builder lore(AnimatedLore lore) {
			this.lore = lore;
			return this;
		}
		
		public Builder frameDelay(int frameDelay) {
			this.frameDelay = frameDelay;
			return this;
		}
		
		public Builder enchantments(Map<Enchantment, Integer> enchantments) {
			this.enchantments = enchantments;
			return this;
		}
		
		public Builder clickListener(ItemClickListener clickListener) {
			this.clickListener = clickListener;
			return this;
		}
		
		public Builder permission(String permission) {
			this.permission = permission;
			return this;
		}
		
		public Builder hiddenWithoutPermission(boolean hiddenWithoutPermission) {
			this.hiddenWithoutPermission = hiddenWithoutPermission;
			return this;
		}
		
		public Builder leatherArmorColor(Color leatherArmorColor) {
			this.leatherArmorColor = leatherArmorColor;
			return this;
		}
		
		public Builder skullOwner(String skullOwner) {
			this.skullOwner = skullOwner;
			return this;
		}
		
		public MenuItemSettings build() {
			MenuItemSettings settings = new MenuItemSettings(name);
			if(material != null) settings.setMaterial(material);
			if(displayName != null) settings.setDisplayName(displayName);
			if(lore != null) settings.setLore(lore);
			if(frameDelay > 0) settings.setFrameDelay(frameDelay);
			if(enchantments != null) settings.setEnchantments(enchantments);
			if(clickListener != null) settings.setClickListener(clickListener);
			if(permission != null) settings.setPermission(permission);
			settings.setHiddenWithoutPermission(hiddenWithoutPermission);
			settings.setLeatherArmorColor(leatherArmorColor);
			settings.setSkullOwner(skullOwner);
			return settings;
		}
	}
}