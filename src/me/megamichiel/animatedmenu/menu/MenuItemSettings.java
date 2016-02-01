package me.megamichiel.animatedmenu.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore;
import me.megamichiel.animatedmenu.animation.AnimatedLore.Frame;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

@AllArgsConstructor @Getter @Setter
public class MenuItemSettings {
	
	private final String name;
	private AnimatedMaterial material;
	private AnimatedName displayName;
	private AnimatedLore lore;
	private int frameDelay;
	private Map<Enchantment, Integer> enchantments;
	private ItemClickListener clickListener;
	private String hidePermission;
	private Color leatherArmorColor;
	private String skullOwner;
	
	public MenuItemSettings(String name) {
		this.name = name;
		material = new AnimatedMaterial();
		displayName = new AnimatedName();
		lore = new AnimatedLore();
		enchantments = new HashMap<Enchantment, Integer>();
		frameDelay = 20;
	}
	
	@SuppressWarnings("deprecation")
	public static MenuItemSettings parse(AnimatedMenuPlugin plugin, AnimatedMenu menu, String itemName, ConfigurationSection section) {
		AnimatedMaterial material = new AnimatedMaterial();
		if (section.isConfigurationSection("Material"))
		{
			material.load(plugin, section.getConfigurationSection("Material"));
		}
		else if (section.contains("Material"))
		{
			Object o = section.get("Material");
			if (!(o instanceof List<?>))
			{
				material.add(parseItemStack(plugin, String.valueOf(o)));
			}
			else material.add(new ItemStack(Material.STONE));
		}
		else
		{
			plugin.nag("Item " + itemName + " in menu " + menu.getName() + " doesn't contain Material!");
			material.add(new ItemStack(Material.STONE));
		}
		AnimatedName name = new AnimatedName();
		if (section.isConfigurationSection("Name"))
		{
			name.load(plugin, section.getConfigurationSection("Name"));
		}
		else if (section.contains("Name"))
		{
			Object o = section.get("Name");
			if (!(o instanceof List<?>))
			{
				name.add(StringUtil.parseBundle(plugin, String.valueOf(o)).colorAmpersands());
			}
			else
			{
				name.add(new StringBundle(plugin));
			}
		}
		else
		{
			plugin.nag("Item " + itemName + " in menu " + menu.getName() + " doesn't contain Name!");
			name.add(new StringBundle(plugin));
		}
		AnimatedLore lore = new AnimatedLore();
		if (section.isConfigurationSection("Lore"))
			lore.load(plugin, section.getConfigurationSection("Lore"));
		else if (section.isList("Lore"))
			lore.add(lore.loadFrame(plugin, section.getStringList("Lore")));
		else lore.add(new Frame());
		Map<Enchantment, Integer> enchantments = new HashMap<>();
		for(String str : section.getStringList("Enchantments")) {
			String[] split = str.split(":");
			Enchantment ench;
			try {
				ench = Enchantment.getById(Integer.parseInt(split[0]));
			} catch (Exception ex) {
				plugin.nag("Invalid enchantment id in " + str + "!");
				continue;
			}
			int level = 1;
			try {
				if(split.length == 2)
					level = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				plugin.nag("Invalid enchantment level in " + str + "!");
			}
			enchantments.put(ench, level);
		}
		ItemClickListener clickListener = new DefaultClickListener(plugin, section);
		Color color = getColor(section.getString("Color"));
		String owner = section.getString("SkullOwner");
		return new MenuItemSettings(itemName, material, name, lore,
				section.getInt("Frame-Delay", 20), enchantments, clickListener,
				section.getString("Hide-Permission"), color, owner);
	}
	
	public boolean isHidden(Player p)
	{
		return hidePermission != null && !p.hasPermission(hidePermission);
	}
	
	public ItemStack applyFirst(ItemStack handle)
	{
		handle.addUnsafeEnchantments(this.enchantments);
		return handle;
	}
	
	public void next()
	{
		getMaterial().next();
		getDisplayName().next();
		getLore().next();
	}
	
	public ItemStack apply(Player p, ItemStack handle)
	{
		ItemStack material = getMaterial().get();
		handle.setType(material.getType());
		handle.setAmount(material.getAmount());
		handle.setDurability(material.getDurability());
		
		ItemMeta meta = handle.getItemMeta();
		meta.setDisplayName(getDisplayName().get().toString(p));
		meta.setLore(getLore().get().toStringList(p));
		if(meta instanceof LeatherArmorMeta) {
			((LeatherArmorMeta) meta).setColor(getLeatherArmorColor());
		} else if(meta instanceof SkullMeta) {
			((SkullMeta) meta).setOwner(getSkullOwner());
		}
		handle.setItemMeta(meta);
		return handle;
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
	
	static ItemStack parseItemStack(AnimatedMenuPlugin plugin, String str) {
		String[] split = str.split(":");
		MaterialMatcher matcher = MaterialMatcher.matcher(split[0]);
		if(!matcher.matches()) {
			plugin.nag("Couldn't find appropiate material for " + split[0] + "! Defaulting to stone");
		}
		ItemStack item = new ItemStack(matcher.get());
		if(split.length > 1) {
			try {
				item.setAmount(Integer.parseInt(split[1]));
			} catch (NumberFormatException ex) {
				plugin.nag("Invalid amount in " + str + "! Defaulting to 1");
			}
			if(split.length > 2) {
				try {
					item.setDurability(Short.parseShort(split[2]));
				} catch (NumberFormatException ex) {
					plugin.nag("Invalid data value in " + str + "! Defaulting to 0");
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
		private String hidePermission;
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
		
		public Builder hidePermission(String hidePermission) {
			this.hidePermission = hidePermission;
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
			if (hidePermission != null) settings.setHidePermission(hidePermission);
			settings.setLeatherArmorColor(leatherArmorColor);
			settings.setSkullOwner(skullOwner);
			return settings;
		}
	}
}