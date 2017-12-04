package me.megamichiel.animatedmenu.menu.config;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedLore;
import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animatedmenu.menu.MenuSession;
import me.megamichiel.animatedmenu.menu.item.IMenuItem;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animatedmenu.util.item.BannerPattern;
import me.megamichiel.animatedmenu.util.item.MaterialParser;
import me.megamichiel.animatedmenu.util.item.MaterialSpecific;
import me.megamichiel.animatedmenu.util.item.Skull;
import me.megamichiel.animationlib.animation.AbsAnimatable;
import me.megamichiel.animationlib.bukkit.nbt.NBTUtil;
import me.megamichiel.animationlib.config.ConfigSection;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.lang.Integer.parseUnsignedInt;

public class ConfigMenuItem implements IMenuItem {

    private static final Consumer<ItemMeta> UNBREAKABLE;

    static {
        Consumer<ItemMeta> action;
        try {
            Method setUnbreakable = ItemMeta.class.getDeclaredMethod("setUnbreakable", boolean.class);
            action = meta -> {
                try {
                    setUnbreakable.invoke(meta, true);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            };
        } catch (NoSuchMethodException old) {
            try {
                Class<?> spigot = Class.forName("org.bukkit.inventory.meta.ItemMeta$Spigot");
                Method getSpigot = spigot.getEnclosingClass().getDeclaredMethod("spigot", spigot),
                  setUnbreakable = spigot.getDeclaredMethod("setUnbreakable", boolean.class);
                action = meta -> {
                    try {
                        setUnbreakable.invoke(getSpigot.invoke(meta), true);
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
    private final int slot;

    private final Predicate<Player> viewPredicate;

    private final AbsAnimatable<IPlaceholder<ItemStack>> material;
    private final AbsAnimatable<StringBundle> displayName = AbsAnimatable.ofText(true);
    private final AnimatedLore lore;

    private final Map<Enchantment, Integer> enchantments = new HashMap<>();
    private final MaterialSpecific specific = new MaterialSpecific();
    private final ItemFlag[] itemFlags;
    private final boolean unbreakable;

    private final ClickHandler clickListener;

    ConfigMenuItem(AnimatedMenuPlugin plugin, ConfigMenuProvider provider, AbstractMenu menu, String name, ConfigSection section) {
        this.plugin = plugin;
        this.name = name;

        AbstractMenu.Type type = menu.getType();
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
            } else {
                throw new IllegalArgumentException("Unknown slot for item " + name + " in menu " + menu.getName() + ": " + section.getString("slot") + "!");
            }
        } else {
            throw new IllegalArgumentException("No slot specified for item " + name + " in menu " + menu.getName() + "!");
        }


        material = AbsAnimatable.of(plugin::parseItemPlaceholder);
        if (material.load(plugin, section, "material").addDefault(IPlaceholder.constant(new ItemStack(Material.STONE)))) {
            plugin.nag("Item " + name + " in menu " + menu.getName() + " doesn't contain Material!");
        }
        if (displayName.load(plugin, section, "name").addDefault(new StringBundle(plugin, name))) {
            plugin.nag("Item " + name + " in menu " + menu.getName() + " doesn't contain Name!");
        }
        (lore = new AnimatedLore(plugin)).load(plugin, section, "lore").addDefault(new StringBundle[0]);

        int i = provider.parseTime(section, "refresh-delay", provider.parseTime(section, "frame-delay", 20));

        material.setDelay(i);
        displayName.setDelay(i);
        lore.setDelay(i);

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
        Color color = getColor(section.getString("color"));
        if (color != null) {
            Color c = color;
            specific.add(LeatherArmorMeta.class, (player, meta, context) -> meta.setColor(c));
        }
        String str = section.getString("skull-owner", section.getString("skullowner"));
        if (str != null) {
            specific.add(SkullMeta.class, new Skull(plugin, str));
        }
        try {
            Class.forName("org.bukkit.inventory.meta.SpawnEggMeta");
            EntityType eggType = EntityType.valueOf(section.getString("egg-type").toUpperCase(Locale.ENGLISH).replace('-', '_'));
            specific.add(SpawnEggMeta.class, (player, meta, context) -> meta.setSpawnedType(eggType));
        } catch (NullPointerException | ClassNotFoundException ex) {
            // No egg type ;c
        } catch (IllegalArgumentException ex) {
            plugin.nag("Unknown egg type: " + section.getStringList("egg-type"));
        }

        try {
            if ((str = section.getString("banner-pattern", section.getString("bannerpattern"))) != null) {
                specific.add(BannerMeta.class, new BannerPattern(plugin, str));
            }
        } catch (IllegalArgumentException ex) {
            plugin.nag("Failed to parse banner pattern '" + str + "'!");
            plugin.nag(ex.getMessage());
        }

        if ((str = section.getString("fireworks-color")) != null && (color = getColor(str)) != null) {
            FireworkEffect effect = FireworkEffect.builder().withColor(color).build();
            specific.add(FireworkEffectMeta.class, (player, meta, context) -> meta.setEffect(effect));
        }

        int hideFlags = 0;
        if (section.isInt("hide-flags")) {
            hideFlags = section.getInt("hide-flags");
        } else if ((str = section.getString("hide-flags")) != null) {
            for (String s : str.split(",")) {
                try {
                    hideFlags |= (1 << ItemFlag.valueOf("HIDE_" + s.trim().toUpperCase(Locale.ENGLISH).replace('-', '_')).ordinal());
                } catch (IllegalArgumentException ex) {
                    plugin.nag("No Hide Flag with name \"" + s + "\" found!");
                }
            }
        }
        ItemFlag[] values = ItemFlag.values();
        for (int index = i = values.length; --index >= 0; --i) {
            if ((hideFlags & (1 << index)) == 0) {
                System.arraycopy(values, 0, values, 1, i++ - 1);
            }
        }
        System.arraycopy(values, i, itemFlags = new ItemFlag[values.length - i], 0, values.length - i);

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

        clickListener = new ClickHandler(provider, menu.getName(), name, section);
    }

    @Override
    public int tick() {
        return material.tick() | displayName.tick() | lore.tick() ? UPDATE_ITEM : 0;
    }

    @Override
    public int getSlot(Player player, MenuSession session) {
        return slot;
    }

    @Override
    public ItemStack getItem(Player player, MenuSession session, ItemStack item) {
        if (!viewPredicate.test(player)) {
            return null;
        }
        PlaceholderContext context = PlaceholderContext.create(plugin);
        ItemMeta meta;

        boolean isNull = item == null;
        if (isNull) {
            if ((item = this.material.get().invoke(plugin, player, context)).getType() == Material.AIR) {
                return null;
            }

            if ((meta = (item = item.clone()).getItemMeta()) == null) {
                return item; // Safety, for air n stuff
            }

            meta.addItemFlags(itemFlags);

            this.enchantments.forEach((ench, lvl) -> meta.addEnchant(ench, lvl, true));

            if (unbreakable) {
                UNBREAKABLE.accept(meta);
            }
        } else {
            ItemStack material = this.material.get().invoke(plugin, player, context);
            if (material.getType() == Material.AIR) {
                return null;
            }
            item.setType(material.getType());
            item.setAmount(material.getAmount());
            item.setDurability(material.getDurability());

            meta = item.getItemMeta();
        }

        meta.setDisplayName(displayName.get().toString(player, context));
        meta.setLore(Arrays.stream(lore.get()).map(sb -> sb.toString(player, context)).collect(Collectors.toList()));
        specific.apply(player, meta, context);

        if (isNull) {
            item.setItemMeta(meta);
        } else {
            NBTUtil util = NBTUtil.getInstance();
            util.setTag(item, util.toTag(meta));
        }

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

    private static Color getColor(String val) {
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
                color = Color.fromRGB(parseUnsignedInt(val, 16));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return color.equals(Bukkit.getItemFactory().getDefaultLeatherColor()) ? null : color;
    }
}
