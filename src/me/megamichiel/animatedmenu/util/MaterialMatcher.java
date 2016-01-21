package me.megamichiel.animatedmenu.util;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Material;

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
}
