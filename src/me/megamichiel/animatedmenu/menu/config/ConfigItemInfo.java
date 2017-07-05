package me.megamichiel.animatedmenu.menu.config;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore;
import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animatedmenu.menu.MenuItem;
import me.megamichiel.animatedmenu.menu.MenuSession;
import me.megamichiel.animatedmenu.menu.MenuType;
import me.megamichiel.animatedmenu.menu.item.ItemInfo;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animatedmenu.util.item.BannerPattern;
import me.megamichiel.animatedmenu.util.item.MaterialParser;
import me.megamichiel.animatedmenu.util.item.MaterialSpecific;
import me.megamichiel.animatedmenu.util.item.Skull;
import me.megamichiel.animationlib.animation.Animatable;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.bukkit.nbt.NBTUtil;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

public class ConfigItemInfo implements ItemInfo {

    private static final Consumer<ItemMeta> UNBREAKABLE;

    static {
        Consumer<ItemMeta> action;
        try {
            Method method = ItemMeta.class.getDeclaredMethod("setUnbreakable", boolean.class);
            action = meta -> {
                try {
                    method.invoke(meta, true);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            };
        } catch (NoSuchMethodException old) {
            try {
                Class<?> spigot = Class.forName("org.bukkit.inventory.meta.ItemMeta$Spigot");
                Method method = spigot.getDeclaredMethod("setUnbreakable", boolean.class),
                    getSpigot = spigot.getEnclosingClass().getDeclaredMethod("spigot", spigot);
                action = meta -> {
                    try {
                        method.invoke(getSpigot.invoke(meta), true);
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                };
            } catch (ClassNotFoundException | NoSuchMethodException noSpigot) {
                NBTUtil nbt = NBTUtil.getInstance();
                action = meta -> nbt.getMap(meta).put("Unbreakable", nbt.TRUE);
            }
        }
        UNBREAKABLE = action;
    }

    private final AnimatedMenuPlugin plugin;
    private final String name;
    private final int slot, frameDelay, refreshDelay;

    private final Predicate<Player> viewPredicate;

    private final Animatable<IPlaceholder<ItemStack>> material;
    private final AnimatedText displayName = new AnimatedText();
    private final AnimatedLore lore = new AnimatedLore();

    private final Map<Enchantment, Integer> enchantments = new HashMap<>();
    private final MaterialSpecific specific = new MaterialSpecific();
    private final ItemFlag[] itemFlags;
    private final boolean unbreakable;

    private final ClickHandler clickListener;

    ConfigItemInfo(AnimatedMenuPlugin plugin, MenuLoader loader, AbstractMenu menu, String name, AbstractConfig section) {
        this.plugin = plugin;
        this.name = name;

        MenuType type = menu.getMenuType();
        if (section.isInt("x") && section.isInt("y")) {
            int x = clampSlot(type.getWidth(),  section.getInt("x")) - 1,
                y = clampSlot(type.getHeight(), section.getInt("y")) - 1;
            slot = y * type.getWidth() + x;
        } else if (section.isInt("slot")) {
            slot = clampSlot(type.getSize(), section.getInt("slot")) - 1;
        } else if (section.isString("slot")) {
            String[] split = section.getString("slot").split(",");
            if (split.length == 2) try {
                int x = clampSlot(type.getWidth(), parseInt(split[0].trim())) - 1,
                    y = clampSlot(type.getHeight(), parseInt(split[1].trim())) - 1;
                slot = y * type.getWidth() + x;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Unknown slot for item " + name + " in menu " + menu.getName() + ": " + section.getString("slot") + "!");
            } else throw new IllegalArgumentException("Unknown slot for item " + name + " in menu " + menu.getName() + ": " + section.getString("slot") + "!");
        } else throw new IllegalArgumentException("No slot specified for item " + name + " in menu " + menu.getName() + "!");

        frameDelay = plugin.parseTime(section, "frame-delay", 20);
        refreshDelay = plugin.parseTime(section, "refresh-delay", frameDelay);

        material = Animatable.of(IPlaceholder.constant(new ItemStack(Material.STONE)), plugin::parseItemPlaceholder);
        if (!material.load(plugin, section, "material")) {
            plugin.nag("Item " + name + " in menu " + menu.getName() + " doesn't contain Material!");
        }
        if (!displayName.load(plugin, section, "name", new StringBundle(plugin, name))) {
            plugin.nag("Item " + name + " in menu " + menu.getName() + " doesn't contain Name!");
        }
        lore.load(plugin, section, "lore");
        for (String str : section.getStringList("enchantments")) {
            String[] split = str.split(":");
            Enchantment ench = MaterialParser.getEnchantment(split[0]);
            if (ench == null) {
                plugin.nag("Invalid enchantment id in " + str + "!");
                continue;
            }
            int level = 1;
            try {
                if (split.length == 2) {
                    level = parseInt(split[1]);
                }
            } catch (NumberFormatException ex) {
                plugin.nag("Invalid enchantment level in " + str + "!");
            }
            enchantments.put(ench, level);
        }
        Color leatherArmorColor = getColor(section.getString("color"));
        if (leatherArmorColor != null) {
            specific.add(LeatherArmorMeta.class, (player, meta) -> meta.setColor(leatherArmorColor));
        }
        String skullOwner = section.getString("skull-owner", section.getString("skullowner"));
        if (skullOwner != null) {
            specific.add(SkullMeta.class, new Skull(plugin, skullOwner));
        }
        try {
            Class.forName("org.bukkit.inventory.meta.SpawnEggMeta");
            EntityType eggType = EntityType.valueOf(section.getString("egg-type").toUpperCase(Locale.ENGLISH).replace('-', '_'));
            specific.add(SpawnEggMeta.class, (player, meta) -> meta.setSpawnedType(eggType));
        } catch (NullPointerException | ClassNotFoundException ex) {
            // No egg type ;c
        } catch (IllegalArgumentException ex) {
            plugin.nag("Unknown egg type: " + section.getStringList("egg-type"));
        }

        try {
            String s = section.getString("banner-pattern", section.getString("bannerpattern"));
            if (s != null) {
                specific.add(BannerMeta.class, new BannerPattern(plugin, s));
            }
        } catch (IllegalArgumentException ex) {
            plugin.nag("Failed to parse banner pattern '" + section.getString("banner-pattern", section.getString("bannerpattern")) + "'!");
            plugin.nag(ex.getMessage());
        }

        String fireworks = section.getString("fireworks-color");
        if (fireworks != null) {
            Color color = getColor(fireworks);
            if (color != null) {
                FireworkEffect effect = FireworkEffect.builder().withColor(color).build();
                specific.add(FireworkEffectMeta.class, (player, meta) -> meta.setEffect(effect));
            }
        }

        int hideFlags = 0;
        if (section.isInt("hide-flags")) {
            hideFlags = section.getInt("hide-flags");
        } else if (section.isString("hide-flags")) {
            String[] split = section.getString("hide-flags").split(", ");
            for (String str : split) {
                try {
                    ItemFlag flag = ItemFlag.valueOf("HIDE_" + str.toUpperCase(Locale.ENGLISH).replace('-', '_'));
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
        String perm = section.getString("view-permission", section.getString("hide-permission"));
        if (perm == null) {
            viewPredicate = player -> true;
        } else if (perm.startsWith("-")) {
            StringBundle sb = StringBundle.parse(plugin, perm.substring(1));
            viewPredicate = player -> !player.hasPermission(sb.toString(player));
        } else {
            StringBundle sb = StringBundle.parse(plugin, perm);
            viewPredicate = player -> player.hasPermission(sb.toString(player));
        }
        unbreakable = Flag.parseBoolean(section.getString("unbreakable"));

        clickListener = new ClickHandler(loader, menu.getName(), name, section);
    }

    @Override
    public int getDelay(DelayType type) {
        switch (type) {
            case FRAME: return frameDelay;
            case REFRESH: return refreshDelay;
            default: return Integer.MAX_VALUE;
        }
    }

    @Override
    public void nextFrame() {
        material.next();
        displayName.next();
        lore.next();
    }

    @Override
    public boolean hasFixedSlot() {
        return true;
    }

    @Override
    public int getSlot(Player player, MenuSession session, MenuItem.SlotContext ctx) {
        return slot;
    }

    @Override
    public ItemStack load(Player player, MenuSession session) {
        if (!viewPredicate.test(player)) {
            return null;
        }
        ItemStack item = this.material.get().invoke(plugin, player).clone();
        if (item.getType() == Material.AIR) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item; // Safety, for air
        }

        meta.setDisplayName(displayName.get().toString(player));
        if (!lore.isEmpty()) {
            meta.setLore(lore.stream(player).collect(Collectors.toList()));
        }

        specific.apply(player, meta);
        meta.addItemFlags(itemFlags);

        this.enchantments.forEach((ench, lvl) -> meta.addEnchant(ench, lvl, true));

        if (unbreakable) {
            UNBREAKABLE.accept(meta);
        }

        item.setItemMeta(meta);

        return item;
    }

    @Override
    public ItemStack apply(Player player, MenuSession session, ItemStack item) {
        if (viewPredicate.test(player)) {
            ItemStack material = this.material.get().invoke(plugin, player);
            if (material.getType() == Material.AIR) return null;
            item.setType(material.getType());
            item.setAmount(material.getAmount());
            item.setDurability(material.getDurability());
        } else return null;

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(displayName.get().toString(player));
        meta.setLore(lore.stream(player).collect(Collectors.toList()));
        specific.apply(player, meta);

        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void click(Player player, MenuSession session, ClickType type) {
        clickListener.click(player, type);
    }

    @Override
    public String toString() {
        return name;
    }

    private static int clampSlot(int max, int val) {
        return val < 1 ? 1 : val > max ? max : val;
    }

    private static final Pattern COLOR_PATTERN = Pattern.compile("([0-9]+),\\s*([0-9]+),\\s*([0-9]+)");

    public static Color getColor(String val) {
        if(val == null) {
            return null;
        }
        Matcher matcher = COLOR_PATTERN.matcher(val);
        Color color;
        if (matcher.matches()) {
            color = Color.fromRGB(parseInt(matcher.group(1)),
                                  parseInt(matcher.group(2)),
                                  parseInt(matcher.group(3)));
        } else {
            try {
                color = Color.fromRGB(parseInt(val, 16));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return color.equals(Bukkit.getItemFactory().getDefaultLeatherColor()) ? null : color;
    }
}
