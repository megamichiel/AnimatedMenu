package me.megamichiel.animatedmenu.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

public class MaterialMatcher {

	private static final Map<String, Material> materials;
	
	static {
		materials = new HashMap<String, Material>();
		
		Map<String, Material> temp = new HashMap<String, Material>();
		
		temp.put("iron bar", Material.IRON_FENCE);
		temp.put("iron bars", Material.IRON_FENCE);
		temp.put("glass pane", Material.THIN_GLASS);
		temp.put("nether wart", Material.NETHER_STALK);
		temp.put("nether warts", Material.NETHER_STALK);
		temp.put("slab", Material.STEP);
		temp.put("double slab", Material.DOUBLE_STEP);
		temp.put("stone brick", Material.SMOOTH_BRICK);
		temp.put("stone bricks", Material.SMOOTH_BRICK);
		temp.put("stone stair", Material.SMOOTH_STAIRS);
		temp.put("stone stairs", Material.SMOOTH_STAIRS);
		temp.put("potato", Material.POTATO_ITEM);
		temp.put("carrot", Material.CARROT_ITEM);
		temp.put("brewing stand", Material.BREWING_STAND_ITEM);
		temp.put("cauldron", Material.CAULDRON_ITEM);
		temp.put("carrot on stick", Material.CARROT_STICK);
		temp.put("carrot on a stick", Material.CARROT_STICK);
		temp.put("cobblestone wall", Material.COBBLE_WALL);

		temp.put("wood slab", Material.WOOD_STEP);
		temp.put("double wood slab", Material.WOOD_DOUBLE_STEP);
		temp.put("repeater", Material.DIODE);
		temp.put("piston", Material.PISTON_BASE);
		temp.put("sticky piston", Material.PISTON_STICKY_BASE);
		temp.put("flower pot", Material.FLOWER_POT_ITEM);
		temp.put("wood showel", Material.WOOD_SPADE);
		temp.put("stone showel", Material.STONE_SPADE);
		temp.put("gold showel", Material.GOLD_SPADE);
		temp.put("iron showel", Material.IRON_SPADE);
		temp.put("diamond showel", Material.DIAMOND_SPADE);
		temp.put("steak", Material.COOKED_BEEF);
		temp.put("cooked porkchop", Material.GRILLED_PORK);
		temp.put("raw porkchop", Material.PORK);
		temp.put("hardened clay", Material.HARD_CLAY);
		temp.put("huge brown mushroom", Material.HUGE_MUSHROOM_1);
		temp.put("huge red mushroom", Material.HUGE_MUSHROOM_2);
		temp.put("mycelium", Material.MYCEL);
		temp.put("poppy", Material.RED_ROSE);
		temp.put("comparator", Material.REDSTONE_COMPARATOR);
		temp.put("skull", Material.SKULL_ITEM);
		temp.put("head", Material.SKULL_ITEM);
		temp.put("redstone torch", Material.REDSTONE_TORCH_ON);
		temp.put("redstone lamp", Material.REDSTONE_LAMP_OFF);
		temp.put("glistering melon", Material.SPECKLED_MELON);
		temp.put("gunpowder", Material.SULPHUR);
		temp.put("lilypad", Material.WATER_LILY);
		temp.put("command block", Material.COMMAND);
		temp.put("dye", Material.INK_SACK);
		
		List<Map.Entry<String, Material>> entries = new ArrayList<>(temp.entrySet());
		for(int i=0;i<entries.size();i++) {
			Map.Entry<String, Material> entry = entries.get(i);
			materials.put(entry.getKey().toUpperCase().replaceAll(" ", ""), entry.getValue());
		}
	}
	
	public static MaterialMatcher matcher(String str) {
		return new MaterialMatcher(str);
	}
	
	private final Material match;
	private final boolean matched;
	
	private MaterialMatcher(String str) {
		String filtered = str.toUpperCase().replaceAll("[-_ ]", ""); 
		Material m = materials.get(filtered);
		if(m == null) {
			m = Material.matchMaterial(str.replace("-", "_"));
		}
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
