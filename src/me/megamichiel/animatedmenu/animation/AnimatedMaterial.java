package me.megamichiel.animatedmenu.animation;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.MaterialMatcher;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.Animatable;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class AnimatedMaterial extends Animatable<IPlaceholder<ItemStack>> {
    
    private static final long serialVersionUID = 3993512547684702735L;

    private final AnimatedMenuPlugin plugin;

    public AnimatedMaterial(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected IPlaceholder<ItemStack> convert(Nagger nagger, Object o) {
        final StringBundle sb = StringBundle.parse(nagger, o.toString());
        if (sb.containsPlaceholders()) {
            return (n, player) -> parseItemStack(plugin, sb.toString(player));
        } else return IPlaceholder.constant(parseItemStack(plugin, sb.toString(null)));
    }
    
    @Override
    protected IPlaceholder<ItemStack> defaultValue() {
        return IPlaceholder.constant(new ItemStack(Material.STONE));
    }

    @Override
    public AnimatedMaterial clone() {
        return (AnimatedMaterial) super.clone();
    }

    public static ItemStack parseItemStack(AnimatedMenuPlugin plugin, String str) {
        String[] split = str.split(":");
        Material type = MaterialMatcher.parse(split[0]);
        if (type == null) {
            plugin.nag("Couldn't find appropiate material for " + split[0] + "! Defaulting to stone");
            type = Material.STONE;
        }
        if (split.length == 1) return new ItemStack(type);
        int amount = 1;
        short data = 0;
        try {
            amount = Integer.parseInt(split[1]);
            if (amount < 0) amount = 0;
            if (amount > 64) amount = 64;
        } catch (NumberFormatException ex) {
            plugin.nag("Invalid amount in " + str + "! Defaulting to 1");
        }
        if (split.length > 2) {
            data = plugin.resolveDataValue(plugin, type, split[2]);
        }
        return new ItemStack(type, amount, data);
    }
}
