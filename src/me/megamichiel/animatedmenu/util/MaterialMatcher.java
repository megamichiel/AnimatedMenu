package me.megamichiel.animatedmenu.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("deprecation")
public class MaterialMatcher {
    
    private static final Method ITEM_BY_NAME, ITEM_TO_MATERIAL, BLOCK_BY_NAME, BLOCK_TO_MATERIAL;
    
    static {
        Method[] methods = new Method[4];
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> item = Class.forName("net.minecraft.server." + version + ".Item");
            Class<?>[] params;
            for (Method m : item.getDeclaredMethods()) {
                if ((params = m.getParameterTypes()).length == 1 &&
                        params[0] == String.class && Modifier.isStatic(m.getModifiers())) {
                    methods[0] = m;
                    break;
                }
            }
            Class<?> cmn = Class.forName("org.bukkit.craftbukkit." + version + ".util.CraftMagicNumbers");
            methods[1] = cmn.getDeclaredMethod("getMaterial", item);
            Class<?> block = Class.forName("net.minecraft.server." + version + ".Block");
            methods[2] = block.getDeclaredMethod("getByName", String.class);
            methods[3] = cmn.getDeclaredMethod("getMaterial", block);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ITEM_BY_NAME = methods[0];
        ITEM_TO_MATERIAL = methods[1];
        BLOCK_BY_NAME = methods[2];
        BLOCK_TO_MATERIAL = methods[3];
    }
    
    public static Material parse(String value) {
        value = value.toLowerCase(Locale.ENGLISH).replace("-", "_");
        try {
            Object o = ITEM_BY_NAME.invoke(null, value);
            if (o != null)
                return (Material) ITEM_TO_MATERIAL.invoke(null, o);
            if (((o = BLOCK_BY_NAME.invoke(null, value))) != null)
                return (Material) BLOCK_TO_MATERIAL.invoke(null, o);
        } catch (Exception ex) {
            // Failed to load in <clinit>
        }
        return Material.matchMaterial(value);
    }
    
    private static final Map<String, Enchantment> enchantments = new HashMap<>();

    static {
        enchantments.put("protection",            Enchantment.PROTECTION_ENVIRONMENTAL);
        enchantments.put("fire_protection",       Enchantment.PROTECTION_FIRE);
        enchantments.put("feather_falling",       Enchantment.PROTECTION_FALL);
        enchantments.put("blast_protection",      Enchantment.PROTECTION_EXPLOSIONS);
        enchantments.put("projectile_protection", Enchantment.PROTECTION_PROJECTILE);
        enchantments.put("respiration",           Enchantment.OXYGEN);
        enchantments.put("aqua_affinity",         Enchantment.WATER_WORKER);
        enchantments.put("thorns",                Enchantment.THORNS);
        enchantments.put("depth_strider",         Enchantment.DEPTH_STRIDER);
        try {
            enchantments.put("frost_walker",      Enchantment.FROST_WALKER);  // 1.9
            enchantments.put("binding_curse",     Enchantment.BINDING_CURSE); // 1.11
        } catch (NoSuchFieldError err) {
            // Nop
        }
        enchantments.put("sharpness",             Enchantment.DAMAGE_ALL);
        enchantments.put("smite",                 Enchantment.DAMAGE_UNDEAD);
        enchantments.put("bane_of_arthropods",    Enchantment.DAMAGE_ARTHROPODS);
        enchantments.put("knockback",             Enchantment.KNOCKBACK);
        enchantments.put("fire_aspect",           Enchantment.FIRE_ASPECT);
        enchantments.put("looting",               Enchantment.LOOT_BONUS_MOBS);
        enchantments.put("efficiency",            Enchantment.DIG_SPEED);
        enchantments.put("silk_touch",            Enchantment.SILK_TOUCH);
        enchantments.put("unbreaking",            Enchantment.DURABILITY);
        enchantments.put("fortune",               Enchantment.LOOT_BONUS_BLOCKS);
        enchantments.put("power",                 Enchantment.ARROW_DAMAGE);
        enchantments.put("punch",                 Enchantment.ARROW_KNOCKBACK);
        enchantments.put("flame",                 Enchantment.ARROW_FIRE);
        enchantments.put("infinity",              Enchantment.ARROW_INFINITE);
        enchantments.put("luck_of_the_sea",       Enchantment.LUCK);
        enchantments.put("lure",                  Enchantment.LURE);
        try {
            enchantments.put("mending",           Enchantment.MENDING);         // 1.9
            enchantments.put("vanishing_curse",   Enchantment.VANISHING_CURSE); // 1.11
        } catch (NoSuchFieldError err) {
            // Nop
        }

        for (Enchantment ench : Enchantment.values())
            enchantments.put(Integer.toString(ench.getId()), ench);
    }
    
    public static Enchantment getEnchantment(String id) {
        return enchantments.get(id.toLowerCase(Locale.ENGLISH).replace("-", "_"));
    }
}
