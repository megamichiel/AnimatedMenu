package me.megamichiel.animatedmenu.util;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
    
    public static MaterialMatcher parse(String value)
    {
        value = value.toLowerCase().replace("-", "_");
        Material m = null;
        try {
            Object o = ITEM_BY_NAME.invoke(null, value);
            if (o != null)
                m = (Material) ITEM_TO_MATERIAL.invoke(null, o);
            else if (((o = BLOCK_BY_NAME.invoke(null, value))) != null)
                m = (Material) BLOCK_TO_MATERIAL.invoke(null, o);
            else m = Material.matchMaterial(value);
        } catch (Exception ex) {
            // No material found ;c
        }
        return new MaterialMatcher(m == null ? Material.STONE : m, m != null);
    }
    
    private final IPlaceholder<Material> match;
    private final boolean matched;
    
    private MaterialMatcher(Material type, boolean matched) {
        match = new IPlaceholder.ConstantPlaceholder<>(type);
        this.matched = matched;
    }
    
    public boolean matches() {
        return matched;
    }
    
    public Material get(Nagger nagger, Player who) {
        return match.invoke(nagger, who);
    }
    
    private static final Map<String, Enchantment> enchantments = new HashMap<>();

    static {
        enchantments.put("power", Enchantment.ARROW_DAMAGE);
        enchantments.put("flame", Enchantment.ARROW_FIRE);
        enchantments.put("infinity", Enchantment.ARROW_INFINITE);
        enchantments.put("punch", Enchantment.ARROW_KNOCKBACK);

        enchantments.put("sharpness", Enchantment.DAMAGE_ALL);
        enchantments.put("bane_of_arthropods", Enchantment.DAMAGE_ARTHROPODS);
        enchantments.put("smite", Enchantment.DAMAGE_UNDEAD);

        enchantments.put("depth_strider", Enchantment.DEPTH_STRIDER);

        enchantments.put("efficiency", Enchantment.DIG_SPEED);
        enchantments.put("unbreaking", Enchantment.DURABILITY);

        enchantments.put("fire_aspect", Enchantment.FIRE_ASPECT);

        try {
            enchantments.put("frost_walker", Enchantment.FROST_WALKER);
            enchantments.put("mending", Enchantment.MENDING);
        } catch (NoSuchFieldError err) {
            // No 1.9, no prob
        }

        enchantments.put("knockback", Enchantment.KNOCKBACK);

        enchantments.put("fortune", Enchantment.LOOT_BONUS_BLOCKS);
        enchantments.put("looting", Enchantment.LOOT_BONUS_MOBS);

        enchantments.put("luck_of_the_sea", Enchantment.LUCK);
        enchantments.put("lure", Enchantment.LURE);
        enchantments.put("mending", Enchantment.MENDING);

        enchantments.put("respiration", Enchantment.OXYGEN);

        enchantments.put("protection", Enchantment.PROTECTION_ENVIRONMENTAL);
        enchantments.put("blast_protection", Enchantment.PROTECTION_EXPLOSIONS);
        enchantments.put("feather_falling", Enchantment.PROTECTION_FALL);
        enchantments.put("fire_protection", Enchantment.PROTECTION_FIRE);
        enchantments.put("projectile_protection", Enchantment.PROTECTION_PROJECTILE);

        enchantments.put("silk_touch", Enchantment.SILK_TOUCH);
        enchantments.put("thorns", Enchantment.THORNS);
        enchantments.put("aqua_affinity", Enchantment.WATER_WORKER);
        for (Enchantment ench : Enchantment.values())
            enchantments.put(Integer.toString(ench.getId()), ench);
    }
    
    public static Enchantment getEnchantment(String id) {
        return enchantments.get(id.toLowerCase().replace("-", "_"));
    }
}
