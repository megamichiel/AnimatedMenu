package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore;
import me.megamichiel.animatedmenu.animation.AnimatedMaterial;
import me.megamichiel.animatedmenu.util.*;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.placeholder.StringBundle;
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
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuItemSettings {
    
    private final String name;
    private AnimatedMaterial material;
    private AnimatedText displayName;
    private AnimatedLore lore;
    private int frameDelay, refreshDelay;
    private Map<Enchantment, Integer> enchantments;
    private ItemClickListener clickListener;
    private StringBundle hidePermission;
    private Color leatherArmorColor;
    private Skull skull;
    private BannerPattern bannerPattern;
    private int hideFlags = 0;
    
    public MenuItemSettings(Plugin plugin, String name) {
        this.name = name;
        material = new AnimatedMaterial();
        displayName = new AnimatedText();
        lore = new AnimatedLore(plugin);
        frameDelay = refreshDelay = 20;
        enchantments = new HashMap<>();
    }

    public MenuItemSettings(String name, AnimatedMaterial material, AnimatedText displayName, AnimatedLore lore, int frameDelay, int refreshDelay, Map<Enchantment, Integer> enchantments, ItemClickListener clickListener, StringBundle hidePermission, Color leatherArmorColor, Skull skull, BannerPattern bannerPattern, int hideFlags) {
        this.name = name;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.frameDelay = frameDelay;
        this.refreshDelay = refreshDelay;
        this.enchantments = enchantments;
        this.clickListener = clickListener;
        this.hidePermission = hidePermission;
        this.leatherArmorColor = leatherArmorColor;
        this.skull = skull;
        this.bannerPattern = bannerPattern;
        this.hideFlags = hideFlags;
    }

    MenuItemSettings load(AnimatedMenuPlugin plugin, String menu, ConfigurationSection section) {
        frameDelay = section.getInt("frame-delay", 20);
        refreshDelay = section.getInt("refresh-delay", frameDelay);
        material = new AnimatedMaterial();
        if (!material.load(plugin, section, "material"))
        {
            plugin.nag("Item " + name + " in menu " + menu + " doesn't contain Material!");
        }
        displayName = new AnimatedText();
        if (!displayName.load(plugin, section, "name", new StringBundle(plugin, name)))
        {
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
        clickListener = new DefaultClickListener(plugin, section);
        leatherArmorColor = getColor(section.getString("color"));
        String skullOwner = section.getString("skullowner");
        skull = skullOwner == null ? null : new Skull(plugin, skullOwner);
        bannerPattern = null;
        try
        {
            bannerPattern = new BannerPattern(plugin, section.getString("bannerpattern"));
        }
        catch (NullPointerException ex) {}
        catch (IllegalArgumentException ex)
        {
            plugin.nag("Failed to parse banner pattern '" + section.getString("bannerpattern") + "'!");
            plugin.nag(ex.getMessage());
        }
        if (section.isInt("hide-flags")) {
            hideFlags = section.getInt("hide-flags");
        } else if (section.isString("hide-flags")) {
            String[] split = section.getString("hide-flags").split(", ");
            for (String str : split) {
                try {
                    ItemFlag flag = ItemFlag.valueOf("HIDE_" + str.toUpperCase().replace('-', '_'));
                    hideFlags |= (1 << flag.ordinal());
                } catch (IllegalArgumentException ex) {
                    plugin.nag("No Hide Flag by name \"" + str + "\" found!");
                }
            }
        }
        hidePermission = StringBundle.parse(plugin, section.getString("hide-permission"));
        return this;
    }
    
    public boolean isHidden(AnimatedMenuPlugin plugin, Player p)
    {
        return hidePermission != null && !p.hasPermission(hidePermission.toString(p));
    }

    public ItemStack first(Nagger nagger, Player who) {
        return applyFirst(nagger, who, material.get().toItemStack(nagger, who));
    }

    public ItemStack applyFirst(Nagger nagger, Player who, ItemStack handle)
    {
        if (this.enchantments != null)
            handle.addUnsafeEnchantments(this.enchantments);
        
        ItemMeta meta = handle.getItemMeta();
        meta.setDisplayName(displayName.get().toString(who));
        meta.setLore(lore.get().toStringList(who));
        if (meta instanceof LeatherArmorMeta && getLeatherArmorColor() != null)
            ((LeatherArmorMeta) meta).setColor(getLeatherArmorColor());
        else if (meta instanceof SkullMeta && getSkull() != null)
            getSkull().apply(who, (SkullMeta) meta);
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
        else if (meta instanceof SkullMeta && getSkull() != null)
            getSkull().apply(p, (SkullMeta) meta);
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
    
    public static Builder builder(Plugin plugin, String name) {
        return new Builder(plugin, name);
    }

    public String getName() {
        return this.name;
    }

    public AnimatedMaterial getMaterial() {
        return this.material;
    }

    public AnimatedText getDisplayName() {
        return this.displayName;
    }

    public AnimatedLore getLore() {
        return this.lore;
    }

    public int getFrameDelay() {
        return this.frameDelay;
    }

    public int getRefreshDelay() {
        return this.refreshDelay;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return this.enchantments;
    }

    public ItemClickListener getClickListener() {
        return this.clickListener;
    }

    public StringBundle getHidePermission() {
        return this.hidePermission;
    }

    public Color getLeatherArmorColor() {
        return this.leatherArmorColor;
    }

    public Skull getSkull() {
        return this.skull;
    }

    public BannerPattern getBannerPattern() {
        return this.bannerPattern;
    }

    public int getHideFlags() {
        return this.hideFlags;
    }

    public void setMaterial(AnimatedMaterial material) {
        this.material = material;
    }

    public void setDisplayName(AnimatedText displayName) {
        this.displayName = displayName;
    }

    public void setLore(AnimatedLore lore) {
        this.lore = lore;
    }

    public void setFrameDelay(int frameDelay) {
        this.frameDelay = frameDelay;
    }

    public void setRefreshDelay(int refreshDelay) {
        this.refreshDelay = refreshDelay;
    }

    public void setEnchantments(Map<Enchantment, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public void setClickListener(ItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setHidePermission(StringBundle hidePermission) {
        this.hidePermission = hidePermission;
    }

    public void setLeatherArmorColor(Color leatherArmorColor) {
        this.leatherArmorColor = leatherArmorColor;
    }

    public void setSkull(Skull skull) {
        this.skull = skull;
    }

    public void setBannerPattern(BannerPattern bannerPattern) {
        this.bannerPattern = bannerPattern;
    }

    public void setHideFlags(int hideFlags) {
        this.hideFlags = hideFlags;
    }

    public static class Builder {

        private final Plugin plugin;
        private final String name;
        private AnimatedMaterial material;
        private AnimatedText displayName;
        private AnimatedLore lore;
        private int frameDelay = 20;
        private Map<Enchantment, Integer> enchantments;
        private ItemClickListener clickListener;
        private StringBundle hidePermission;
        private Color leatherArmorColor;
        private Skull skull;
        private int hideFlags;

        private Builder(Plugin plugin, String name) {
            this.plugin = plugin;
            this.name = name;
        }

        public MenuItemSettings build() {
            MenuItemSettings settings = new MenuItemSettings(plugin, name);
            if(material != null) settings.setMaterial(material);
            if(displayName != null) settings.setDisplayName(displayName);
            if(lore != null) settings.setLore(lore);
            settings.setFrameDelay(frameDelay);
            settings.setEnchantments(enchantments);
            settings.setClickListener(clickListener);
            settings.setHidePermission(hidePermission);
            settings.setLeatherArmorColor(leatherArmorColor);
            settings.setSkull(skull);
            settings.setHideFlags(hideFlags);
            return settings;
        }

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

        public Builder skull(Skull skull) {
            this.skull = skull;
            return this;
        }

        public Builder hideFlags(int hideFlags) {
            this.hideFlags = hideFlags;
            return this;
        }
    }
}
