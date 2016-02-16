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
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore;
import me.megamichiel.animatedmenu.animation.AnimatedMaterial;
import me.megamichiel.animatedmenu.animation.AnimatedText;
import me.megamichiel.animatedmenu.util.BannerPattern;
import me.megamichiel.animatedmenu.util.MaterialMatcher;
import me.megamichiel.animatedmenu.util.Nagger;
import me.megamichiel.animatedmenu.util.StringBundle;
import me.megamichiel.animatedmenu.util.StringUtil;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

@AllArgsConstructor @Getter @Setter
public class MenuItemSettings {
	
	private final String name;
	private AnimatedMaterial material;
	private AnimatedText displayName;
	private AnimatedLore lore;
	private int frameDelay;
	private Map<Enchantment, Integer> enchantments;
	private ItemClickListener clickListener;
	private StringBundle hidePermission;
	private Color leatherArmorColor;
	private StringBundle skullOwner;
	private BannerPattern bannerPattern;
	private int hideFlags = 0;
	
	public MenuItemSettings(String name) {
		this.name = name;
		material = new AnimatedMaterial();
		displayName = new AnimatedText();
		lore = new AnimatedLore();
		frameDelay = 20;
		enchantments = new HashMap<Enchantment, Integer>();
	}
	
	public MenuItemSettings load(AnimatedMenuPlugin plugin, String menu, ConfigurationSection section) {
		frameDelay = section.getInt("Frame-Delay", 20);
		material = new AnimatedMaterial();
		if (!material.load(plugin, section, "Material"))
		{
			plugin.nag("Item " + name + " in menu " + menu + " doesn't contain Material!");
		}
		displayName = new AnimatedText();
		if (!displayName.load(plugin, section, "Name", new StringBundle(plugin, name)))
		{
			plugin.nag("Item " + name + " in menu " + menu + " doesn't contain Name!");
		}
		lore = new AnimatedLore();
		lore.load(plugin, section, "Lore");
		enchantments = new HashMap<>();
		for (String str : section.getStringList("Enchantments")) {
			String[] split = str.split(":");
			Enchantment ench;
			try {
				ench = MaterialMatcher.getEnchantment(split[0]);
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
		clickListener = new DefaultClickListener(plugin, section);
		leatherArmorColor = getColor(section.getString("Color"));
		skullOwner = StringUtil.parseBundle(plugin, section.getString("SkullOwner"));
		bannerPattern = null;
		try
		{
			bannerPattern = new BannerPattern(plugin, section.getString("BannerPattern"));
		}
		catch (NullPointerException ex) {}
		catch (IllegalArgumentException ex)
		{
			plugin.nag("Failed to parse banner pattern '" + section.getString("BannerPattern") + "'!");
			plugin.nag(ex.getMessage());
		}
		hideFlags = section.getInt("Hide-Flags");
		hidePermission = StringUtil.parseBundle(plugin, section.getString("Hide-Permission"));
		return this;
	}
	
	public boolean isHidden(Player p)
	{
		return hidePermission != null && !p.hasPermission(hidePermission.toString(p));
	}
	
	public ItemStack applyFirst(Nagger nagger, Player who, ItemStack handle)
	{
		if (this.enchantments != null)
			handle.addUnsafeEnchantments(this.enchantments);
		
		ItemMeta meta = handle.getItemMeta();
		if (meta instanceof LeatherArmorMeta && getLeatherArmorColor() != null)
			((LeatherArmorMeta) meta).setColor(getLeatherArmorColor());
		else if (meta instanceof SkullMeta && getSkullOwner() != null)
			((SkullMeta) meta).setOwner(getSkullOwner().toString(who));
		else if (meta instanceof BannerMeta && getBannerPattern() != null)
			getBannerPattern().apply((BannerMeta) meta);
		for (ItemFlag itemFlag : ItemFlag.values())
		{
			if ((hideFlags & (1 << itemFlag.ordinal())) != 0)
				meta.addItemFlags(itemFlag);
			else meta.removeItemFlags(itemFlag);
		}
		
		handle.setItemMeta(meta);
		
		return handle;
	}
	
	public void next()
	{
		getMaterial().next();
		getDisplayName().next();
		getLore().next();
	}
	
	public ItemStack apply(Nagger nagger, Player p, ItemStack handle)
	{
		ItemStack material = getMaterial().get().toItemStack(nagger, p);
		handle.setType(material.getType());
		handle.setAmount(material.getAmount());
		handle.setDurability(material.getDurability());
		
		ItemMeta meta = handle.getItemMeta();
		meta.setDisplayName(getDisplayName().get().toString(p));
		if (!getLore().isEmpty())
			meta.setLore(getLore().get().toStringList(p));
		
		if (meta instanceof LeatherArmorMeta && getLeatherArmorColor() != null)
			((LeatherArmorMeta) meta).setColor(getLeatherArmorColor());
		else if (meta instanceof SkullMeta && getSkullOwner() != null)
			((SkullMeta) meta).setOwner(getSkullOwner().toString(p));
		else if (meta instanceof BannerMeta && getBannerPattern() != null)
			getBannerPattern().apply((BannerMeta) meta);
		for (ItemFlag itemFlag : ItemFlag.values())
		{
			if ((hideFlags & (1 << itemFlag.ordinal())) != 0)
				meta.addItemFlags(itemFlag);
			else meta.removeItemFlags(itemFlag);
		}
		handle.setItemMeta(meta);
		
		return handle;
	}
	
	private static final Pattern COLOR_PATTERN = Pattern.compile("([0-9]+),\\s*([0-9]+),\\s*([0-9]+)");
	
	private Color getColor(String val) {
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
		private AnimatedText displayName;
		private AnimatedLore lore;
		private int frameDelay = 20;
		private Map<Enchantment, Integer> enchantments;
		private ItemClickListener clickListener;
		private StringBundle hidePermission;
		private Color leatherArmorColor;
		private StringBundle skullOwner;
		private int hideFlags;
		
		public Builder material(AnimatedMaterial material) {
			this.material = material;
			return this;
		}
		
		public Builder displayName(AnimatedText displayName) {
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
		
		public Builder hidePermission(StringBundle hidePermission) {
			this.hidePermission = hidePermission;
			return this;
		}
		
		public Builder leatherArmorColor(Color leatherArmorColor) {
			this.leatherArmorColor = leatherArmorColor;
			return this;
		}
		
		public Builder skullOwner(StringBundle skullOwner) {
			this.skullOwner = skullOwner;
			return this;
		}
		
		public Builder hideFlags(int hideFlags) {
			this.hideFlags = hideFlags;
			return this;
		}
		
		public MenuItemSettings build() {
			MenuItemSettings settings = new MenuItemSettings(name);
			if(material != null) settings.setMaterial(material);
			if(displayName != null) settings.setDisplayName(displayName);
			if(lore != null) settings.setLore(lore);
			settings.setFrameDelay(frameDelay);
			settings.setEnchantments(enchantments);
			settings.setClickListener(clickListener);
			settings.setHidePermission(hidePermission);
			settings.setLeatherArmorColor(leatherArmorColor);
			settings.setSkullOwner(skullOwner);
			settings.setHideFlags(hideFlags);
			return settings;
		}
	}
}