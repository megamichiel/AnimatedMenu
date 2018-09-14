package me.megamichiel.animatedmenu.util.item;

import me.megamichiel.animationlib.bukkit.AnimLibPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

//@SuppressWarnings("deprecation")
public class MaterialParser {
    
    private static final Method ITEM_BY_NAME, ITEM_TO_MATERIAL, BLOCK_BY_NAME, BLOCK_TO_MATERIAL, LEGACY_MATCH, GET_KEY;
    private static final Constructor<?> MINECRAFT_KEY;
    private static final Field REGISTRY_BLOCK;
    
    static {
        Method[] methods = new Method[4];
        Constructor<?> minecraftKey = null;
        Field registryBlock = null;
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
            try {
                methods[2] = block.getDeclaredMethod("getByName", String.class);
            } catch (Exception ex) {
                Class<?> registry = Class.forName("net.minecraft.server." + version + ".IRegistry");
                minecraftKey = Class.forName("net.minecraft.server." + version + ".MinecraftKey").getDeclaredConstructor(String.class);
                registryBlock = registry.getDeclaredField("BLOCK");
                methods[2] = registry.getDeclaredMethod("getOrDefault", minecraftKey.getDeclaringClass());
            }
            methods[3] = cmn.getDeclaredMethod("getMaterial", block);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ITEM_BY_NAME = methods[0];
        ITEM_TO_MATERIAL = methods[1];
        BLOCK_BY_NAME = methods[2];
        BLOCK_TO_MATERIAL = methods[3];

        MINECRAFT_KEY = minecraftKey;
        REGISTRY_BLOCK = registryBlock;

        Method legacy = null, getKey = null;
        try {
            legacy = Material.class.getDeclaredMethod("matchMaterial", String.class, boolean.class);
            getKey = Enchantment.class.getDeclaredMethod("getKey");
        } catch (Exception ex) {
            //
        }
        LEGACY_MATCH = legacy;
        GET_KEY = getKey;
    }
    
    public static Material parse(String value) {
        String id = value.toLowerCase(Locale.ENGLISH).replace('-', '_');
        try {
            Object o; Material m;
            if (((o =  ITEM_BY_NAME.invoke(null, id)) != null && (m = (Material) ITEM_TO_MATERIAL.invoke(null, o)) != null && m != Material.AIR)) {
                return m; // an item by the name was found
            }
            if (MINECRAFT_KEY != null) {
                o = BLOCK_BY_NAME.invoke(REGISTRY_BLOCK.get(null), MINECRAFT_KEY.newInstance(id));
            } else {
                o = BLOCK_BY_NAME.invoke(null, id);
            }
            if ((o != null && (m = (Material) BLOCK_TO_MATERIAL.invoke(null, o)) != null && m != Material.AIR)) {
                return m; // a block by the name was found
            }
        } catch (Exception ex) {
            // Failed to load in <clinit>
        }

        Material m = Material.matchMaterial(id);
        if (m == null && LEGACY_MATCH != null) {
            try {
                return (Material) LEGACY_MATCH.invoke(null, id, Boolean.TRUE);
            } catch (Exception ex) {
                return null;
            }
        }
        return m;
    }
    
    private static final Map<String, Enchantment> enchantments = new HashMap<>();
    private static final Method GET_ID;

    static {
        Map<String, Enchantment> map = enchantments;

        map.put("protection",            Enchantment.PROTECTION_ENVIRONMENTAL);
        map.put("fire_protection",       Enchantment.PROTECTION_FIRE);
        map.put("feather_falling",       Enchantment.PROTECTION_FALL);
        map.put("blast_protection",      Enchantment.PROTECTION_EXPLOSIONS);
        map.put("projectile_protection", Enchantment.PROTECTION_PROJECTILE);
        map.put("respiration",           Enchantment.OXYGEN);
        map.put("aqua_affinity",         Enchantment.WATER_WORKER);
        map.put("thorns",                Enchantment.THORNS);
        map.put("depth_strider",         Enchantment.DEPTH_STRIDER);
        try {
            map.put("frost_walker",      Enchantment.FROST_WALKER);  // 1.9
            map.put("binding_curse",     Enchantment.BINDING_CURSE); // 1.11
        } catch (NoSuchFieldError err) {
            // Nop
        }
        map.put("sharpness",             Enchantment.DAMAGE_ALL);
        map.put("smite",                 Enchantment.DAMAGE_UNDEAD);
        map.put("bane_of_arthropods",    Enchantment.DAMAGE_ARTHROPODS);
        map.put("knockback",             Enchantment.KNOCKBACK);
        map.put("fire_aspect",           Enchantment.FIRE_ASPECT);
        map.put("looting",               Enchantment.LOOT_BONUS_MOBS);
        map.put("efficiency",            Enchantment.DIG_SPEED);
        map.put("silk_touch",            Enchantment.SILK_TOUCH);
        map.put("unbreaking",            Enchantment.DURABILITY);
        map.put("fortune",               Enchantment.LOOT_BONUS_BLOCKS);
        map.put("power",                 Enchantment.ARROW_DAMAGE);
        map.put("punch",                 Enchantment.ARROW_KNOCKBACK);
        map.put("flame",                 Enchantment.ARROW_FIRE);
        map.put("infinity",              Enchantment.ARROW_INFINITE);
        map.put("luck_of_the_sea",       Enchantment.LUCK);
        map.put("lure",                  Enchantment.LURE);
        try {
            map.put("mending",           Enchantment.MENDING);         // 1.9
            map.put("vanishing_curse",   Enchantment.VANISHING_CURSE); // 1.11
        } catch (NoSuchFieldError err) {
            // Nop
        }

        Method getId = null;
        if (AnimLibPlugin.IS_LEGACY) {
            try {
                getId = Enchantment.class.getDeclaredMethod("getId");
            } catch (Exception ex) {
                // idk man
            }
        }
        GET_ID = getId;
        String name;
        for (Enchantment ench : Enchantment.values()) {
            if (ench != null) { // Weird, I know. Some person apparently had issues
                try {
                    name = GET_KEY == null ? ench.getName() : ((NamespacedKey) GET_KEY.invoke(ench)).getKey();
                } catch (Exception ex) {
                    continue;
                }
                if (name != null) {
                    if (getId != null) {
                        try {
                            map.put(getId.invoke(ench).toString(), ench);
                        } catch (Exception ex) {
                            // D:
                        }
                    }
                    map.putIfAbsent(name.toLowerCase(Locale.ENGLISH), ench);
                }
            }
        }
    }

    public static int getId(Enchantment enchantment) {
        try {
            return (Integer) GET_ID.invoke(enchantment);
        } catch (Exception ex) {
            return -1;
        }
    }
    
    public static Enchantment getEnchantment(String id) {
        return enchantments.get(id.toLowerCase(Locale.ENGLISH).replace("-", "_"));
    }
}
