package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore;
import me.megamichiel.animatedmenu.animation.AnimatedMaterial;
import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animatedmenu.menu.ItemInfo;
import me.megamichiel.animatedmenu.menu.MenuType;
import me.megamichiel.animatedmenu.util.BannerPattern;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animatedmenu.util.MaterialMatcher;
import me.megamichiel.animatedmenu.util.Skull;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.bukkit.nbt.NBTUtil;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigItemInfo implements ItemInfo {

    private final AnimatedMenuPlugin plugin;
    private final int slot, frameDelay, refreshDelay;

    private final ClickHandler clickListener;
    private final StringBundle hidePermission;
    private final boolean negateHidePermission;

    private final AnimatedMaterial material = new AnimatedMaterial();
    private final AnimatedText displayName = new AnimatedText();
    private final AnimatedLore lore = new AnimatedLore();
    private final Map<Enchantment, Integer> enchantments = new HashMap<>();
    private final Color leatherArmorColor;
    private final Skull skull;
    private final BannerPattern bannerPattern;
    private final ItemFlag[] itemFlags;
    private final boolean unbreakable;

    public ConfigItemInfo(AnimatedMenuPlugin plugin, AbstractMenu menu,
                          String name, AbstractConfig section) {
        this.plugin = plugin;

        MenuType type = menu.getMenuType();
        if (section.isInt("x") && section.isInt("y")) {
            int x = clampSlot(type.getWidth(),  section.getInt("x")) - 1,
                y = clampSlot(type.getHeight(), section.getInt("y")) - 1;
            slot = y * type.getWidth() + x;
        } else if (section.isInt("slot"))
            slot = clampSlot(type.getSize(), section.getInt("slot")) - 1;
        else if (section.isString("slot")) {
            String[] split = section.getString("slot").split(",");
            if (split.length == 2) try {
                int x = clampSlot(type.getWidth(), Integer.parseInt(split[0].trim())) - 1,
                    y = clampSlot(type.getHeight(), Integer.parseInt(split[1].trim())) - 1;
                slot = y * type.getWidth() + x;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Unknown slot for item " + name + " in menu " + menu.getName() + ": " + section.getString("slot") + "!");
            } else throw new IllegalArgumentException("Unknown slot for item " + name + " in menu " + menu.getName() + ": " + section.getString("slot") + "!");
        } else throw new IllegalArgumentException("No slot specified for item " + name + " in menu " + menu.getName() + "!");

        frameDelay = section.getInt("frame-delay", 20);
        refreshDelay = section.getInt("refresh-delay", frameDelay);

        if (!material.load(plugin, section, "material"))
            plugin.nag("Item " + name + " in menu " + menu.getName() + " doesn't contain Material!");
        if (!displayName.load(plugin, section, "name", new StringBundle(plugin, name)))
            plugin.nag("Item " + name + " in menu " + menu.getName() + " doesn't contain Name!");
        lore.load(plugin, section, "lore");
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
        clickListener = new ClickHandler(plugin, menu.getName(), name, section);
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
        List<ItemFlag> flags = new ArrayList<>();
        ItemFlag[] values = ItemFlag.values();
        for (int i = 0, len = values.length; i < len; i++)
            if ((hideFlags & (1 << i)) != 0) flags.add(values[i]);
        itemFlags = flags.toArray(new ItemFlag[flags.size()]);
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
        unbreakable = Flag.parseBoolean(section.getString("unbreakable"));
    }

    @Override
    public int getDelay(boolean refresh) {
        return refresh ? refreshDelay : frameDelay;
    }

    @Override
    public void nextFrame() {
        material.next();
        displayName.next();
        lore.next();
    }

    @Override
    public boolean hasDynamicSlot() {
        return false;
    }

    @Override
    public int getSlot(Player player, SlotContext ctx) {
        return slot;
    }

    private boolean isHidden(Player player) {
        return (hidePermission == null || player.hasPermission(hidePermission.toString(player))) == negateHidePermission;
    }

    @Override
    public ItemStack load(Player player) {
        if (isHidden(player)) return null;
        ItemStack item = this.material.get().invoke(plugin, player).clone();
        if (item.getType() == Material.AIR) return null;
        if (unbreakable) {
            NBTUtil nbt = NBTUtil.getInstance();
            if (item != (item = nbt.asNMS(item))) try {
                nbt.getOrCreateTagMap(item).put("Unbreakable", nbt.TRUE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item; // Safety, for air

        meta.setDisplayName(displayName.get().toString(player));
        if (!lore.isEmpty()) meta.setLore(lore.get().toStringList(plugin, player));

        if (meta instanceof LeatherArmorMeta && leatherArmorColor != null)
            ((LeatherArmorMeta) meta).setColor(leatherArmorColor);
        else if (meta instanceof SkullMeta && skull != null)
            skull.apply(player, (SkullMeta) meta);
        else if (meta instanceof BannerMeta)
            bannerPattern.apply((BannerMeta) meta);
        meta.addItemFlags(itemFlags);

        this.enchantments.forEach((ench, lvl) -> meta.addEnchant(ench, lvl, true));

        item.setItemMeta(meta);

        return item;
    }

    @Override
    public ItemStack apply(Player player, ItemStack item) {
        if (isHidden(player)) return null;
        else {
            ItemStack material = this.material.get().invoke(plugin, player);
            if (material.getType() == Material.AIR) return null;
            item.setType(material.getType());
            item.setAmount(material.getAmount());
            item.setDurability(material.getDurability());
        }

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(displayName.get().toString(player));
        if (!lore.isEmpty()) meta.setLore(lore.get().toStringList(plugin, player));
        if (meta instanceof SkullMeta && skull != null)
            skull.apply(player, (SkullMeta) meta);

        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void click(Player player, ClickType type) {
        clickListener.click(player, type);
    }

    private static int clampSlot(int max, int val) {
        return val < 1 ? 1 : val > max ? max : val;
    }

    private static final Pattern COLOR_PATTERN = Pattern.compile("([0-9]+),\\s*([0-9]+),\\s*([0-9]+)");

    private static Color getColor(String val) {
        if(val == null) return null;
        Matcher matcher = COLOR_PATTERN.matcher(val);
        Color color;
        if (matcher.matches()) {
            color = Color.fromRGB(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        } else try {
            color = Color.fromRGB(Integer.parseInt(val, 16));
        } catch (NumberFormatException ex) {
            return null;
        }
        if (color.equals(Bukkit.getItemFactory().getDefaultLeatherColor()))
            return null;
        return color;
    }
}
