package me.megamichiel.animatedmenu.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

@SuppressWarnings("deprecation")
public class MaterialMatcher {
	
	private static final Method ITEM_BY_NAME, ITEM_TO_MATERIAL, BLOCK_BY_NAME, BLOCK_TO_MATERIAL;
	
	static {
		Method[] methods = new Method[4];
		try
		{
			String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
			Class<?> item = Class.forName("net.minecraft.server." + version + ".Item");
			methods[0] = item.getDeclaredMethod("d", String.class);
			Class<?> cmn = Class.forName("org.bukkit.craftbukkit." + version + ".util.CraftMagicNumbers");
			methods[1] = cmn.getDeclaredMethod("getMaterial", item);
			Class<?> block = Class.forName("net.minecraft.server." + version + ".Block");
			methods[2] = block.getDeclaredMethod("getByName", String.class);
			methods[3] = cmn.getDeclaredMethod("getMaterial", block);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		ITEM_BY_NAME = methods[0];
		ITEM_TO_MATERIAL = methods[1];
		BLOCK_BY_NAME = methods[2];
		BLOCK_TO_MATERIAL = methods[3];
	}
	
	public static MaterialMatcher matcher(String str) {
		return new MaterialMatcher(str);
	}
	
	private final Material match;
	private final boolean matched;
	
	private MaterialMatcher(String value) {
		value = value.replace("-", "_");
		Material m = Material.AIR;
		try
		{
			Object o = ITEM_BY_NAME.invoke(null, value);
			if (o != null)
			{
				m = (Material) ITEM_TO_MATERIAL.invoke(null, o);
			}
			else if (((o = BLOCK_BY_NAME.invoke(null, value))) != null)
			{
				m = (Material) BLOCK_TO_MATERIAL.invoke(null, o);
			}
			else
			{
				m = Material.matchMaterial(value);
			}
		}
		catch (Exception ex) {}
		match = m == null ? Material.STONE : m;
		matched = m != null;
	}
	
	public boolean matches() {
		return matched;
	}
	
	public Material get() {
		return match;
	}
	
	private static final Map<String, Enchantment> enchantments = new HashMap<>();
	
	static
	{
		enchantments.put("protection", Enchantment.PROTECTION_ENVIRONMENTAL);
		enchantments.put("fire_protection", Enchantment.PROTECTION_FIRE);
		enchantments.put("feather_falling", Enchantment.PROTECTION_FALL);
		enchantments.put("blast_protection", Enchantment.PROTECTION_EXPLOSIONS);
		enchantments.put("projectile_protection", Enchantment.PROTECTION_PROJECTILE);
		enchantments.put("respiration", Enchantment.OXYGEN);
		enchantments.put("aqua_affinity", Enchantment.WATER_WORKER);
		enchantments.put("thorns", Enchantment.THORNS);
		enchantments.put("depth_strider", Enchantment.DEPTH_STRIDER);
		enchantments.put("sharpness", Enchantment.DAMAGE_ALL);
		enchantments.put("smite", Enchantment.DAMAGE_UNDEAD);
		enchantments.put("bane_of_arthropods", Enchantment.DAMAGE_ARTHROPODS);
		enchantments.put("knockback", Enchantment.KNOCKBACK);
		enchantments.put("fire_aspect", Enchantment.FIRE_ASPECT);
		enchantments.put("looting", Enchantment.LOOT_BONUS_MOBS);
		enchantments.put("efficiency", Enchantment.DIG_SPEED);
		enchantments.put("silk_touch", Enchantment.SILK_TOUCH);
		enchantments.put("unbreaking", Enchantment.DURABILITY);
		enchantments.put("fortune", Enchantment.LOOT_BONUS_BLOCKS);
		enchantments.put("power", Enchantment.ARROW_DAMAGE);
		enchantments.put("punch", Enchantment.ARROW_KNOCKBACK);
		enchantments.put("luck_of_the_sea", Enchantment.LUCK);
		enchantments.put("lure", Enchantment.LURE);
		for (Enchantment ench : Enchantment.values())
			enchantments.put(String.valueOf(ench.getId()), ench);
	}
	
	public static Enchantment getEnchantment(String id)
	{
		return enchantments.get(id.toLowerCase().replace("-", "_"));
	}
}
