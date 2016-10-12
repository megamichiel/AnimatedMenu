package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore;
import me.megamichiel.animatedmenu.animation.AnimatedMaterial;
import me.megamichiel.animatedmenu.util.BannerPattern;
import me.megamichiel.animatedmenu.util.MaterialMatcher;
import me.megamichiel.animatedmenu.util.Skull;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.bukkit.NBTUtil;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuItemSettings {
    
    private final String name;
    private final AnimatedMaterial material;
    private final AnimatedText displayName;
    private final AnimatedLore lore;
    private final int frameDelay, refreshDelay;
    private final Map<Enchantment, Integer> enchantments;
    final ItemClickListener clickListener;
    private final StringBundle hidePermission;
    private final boolean negateHidePermission;
    private final Color leatherArmorColor;
    private final Skull skull;
    private final BannerPattern bannerPattern;
    private final int hideFlags;
    private final boolean unbreakable;

    MenuItemSettings(AnimatedMenuPlugin plugin, String name,
                            String menu, AbstractConfig section) {
        this.name = name;
        frameDelay = section.getInt("frame-delay", 20);
        refreshDelay = section.getInt("refresh-delay", frameDelay);
        material = new AnimatedMaterial();
        if (!material.load(plugin, section, "material")) {
            plugin.nag("Item " + name + " in menu " + menu + " doesn't contain Material!");
        }
        displayName = new AnimatedText();
        if (!displayName.load(plugin, section, "name", new StringBundle(plugin, name))) {
            plugin.nag("Item " + name + " in menu " + menu + " doesn't contain Name!");
        }
        lore = new AnimatedLore(plugin);
        lore.load(plugin, section, "lore");
        enchantments = new HashMap<>();
        for (String str : section.getStringList("enchantments")) {
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
        clickListener = new ItemClickListener(plugin, section);
        leatherArmorColor = getColor(section.getString("color"));
        String skullOwner = section.getString("skullowner");
        skull = skullOwner == null ? null : new Skull(plugin, skullOwner);
        BannerPattern pattern = BannerPattern.EMPTY;
        try {
            String s = section.getString("bannerpattern");
            if (s != null)
                pattern = new BannerPattern(plugin, s);
        } catch (IllegalArgumentException ex) {
            plugin.nag("Failed to parse banner pattern '" + section.getString("bannerpattern") + "'!");
            plugin.nag(ex.getMessage());
        }
        bannerPattern = pattern;
        int hideFlags = 0;
        if (section.isInt("hide-flags")) {
            hideFlags = section.getInt("hide-flags");
        } else if (section.isString("hide-flags")) {
            String[] split = section.getString("hide-flags").split(", ");
            for (String str : split) {
                try {
                    ItemFlag flag = ItemFlag.valueOf("HIDE_" + str.toUpperCase(Locale.US).replace('-', '_'));
                    hideFlags |= (1 << flag.ordinal());
                } catch (IllegalArgumentException ex) {
                    plugin.nag("No Hide Flag by name \"" + str + "\" found!");
                }
            }
        }
        this.hideFlags = hideFlags;
        String perm = section.getString("hide-permission");
        if (perm == null) {
            hidePermission = null;
            negateHidePermission = false;
        } else if (perm.startsWith("-")) {
            hidePermission = StringBundle.parse(plugin, perm.substring(1));
            negateHidePermission = true;
        } else {
            hidePermission = StringBundle.parse(plugin, perm);
            negateHidePermission = false;
        }
        unbreakable = section.getBoolean("unbreakable");
    }
    
    boolean isHidden(AnimatedMenuPlugin plugin, Player p) {
        if (hidePermission != null)
            return p.hasPermission(hidePermission.toString(p)) == negateHidePermission;
        return negateHidePermission;
    }

    ItemStack getItem(Nagger nagger, Player who) {
        return apply(nagger, who, new ItemStack(Material.AIR));
    }

    ItemStack first(Nagger nagger, Player who) {
        ItemStack handle = this.material.get().invoke(nagger, who);
        if (unbreakable) {
            NBTUtil nbt = NBTUtil.getInstance();
            if (handle != (handle = nbt.asNMS(handle)))
                nbt.getOrCreateTagMap(handle).put("Unbreakable", nbt.TRUE);
            else handle = new ItemStack(handle.getType(), handle.getAmount(), handle.getDurability());
        } else handle = new ItemStack(handle.getType(), handle.getAmount(), handle.getDurability());

        if (this.enchantments != null)
            handle.addUnsafeEnchantments(this.enchantments);
        
        ItemMeta meta = handle.getItemMeta();
        meta.setDisplayName(displayName.get().toString(who));
        meta.setLore(lore.get().toStringList(nagger, who));
        if (meta instanceof LeatherArmorMeta && leatherArmorColor != null)
            ((LeatherArmorMeta) meta).setColor(leatherArmorColor);
        else if (meta instanceof SkullMeta && skull != null)
            skull.apply(who, (SkullMeta) meta);
        else if (meta instanceof BannerMeta && bannerPattern != null)
            bannerPattern.apply((BannerMeta) meta);
        for (ItemFlag itemFlag : ItemFlag.values()) {
            if ((hideFlags & (1 << itemFlag.ordinal())) != 0)
                meta.addItemFlags(itemFlag);
            else meta.removeItemFlags(itemFlag);
        }
        
        handle.setItemMeta(meta);
        
        return handle;
    }
    
    public void next() {
        material.next();
        displayName.next();
        lore.next();
    }
    
    public ItemStack apply(Nagger nagger, Player p, ItemStack handle) {
        if (unbreakable) {
            NBTUtil nbt = NBTUtil.getInstance();
            handle = nbt.asNMS(handle);
            if (nbt.isSupported(handle)) try {
                nbt.getOrCreateTagMap(handle).put("Unbreakable", nbt.TRUE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ItemStack material = this.material.get().invoke(nagger, p);
        handle.setType(material.getType());
        handle.setAmount(material.getAmount());
        handle.setDurability(material.getDurability());
        
        ItemMeta meta = handle.getItemMeta();
        meta.setDisplayName(displayName.get().toString(p));
        if (!lore.isEmpty())
            meta.setLore(lore.get().toStringList(nagger, p));
        
        if (meta instanceof LeatherArmorMeta && leatherArmorColor != null)
            ((LeatherArmorMeta) meta).setColor(leatherArmorColor);
        else if (meta instanceof SkullMeta && skull != null)
            skull.apply(p, (SkullMeta) meta);
        else if (meta instanceof BannerMeta && bannerPattern != null)
            bannerPattern.apply((BannerMeta) meta);
        for (ItemFlag itemFlag : ItemFlag.values()) {
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
        if (matcher.matches()) {
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

    public String getName() {
        return this.name;
    }

    int getFrameDelay() {
        return frameDelay;
    }

    int getRefreshDelay() {
        return refreshDelay;
    }
}
