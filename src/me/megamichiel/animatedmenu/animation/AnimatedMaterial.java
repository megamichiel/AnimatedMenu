package me.megamichiel.animatedmenu.animation;

import me.megamichiel.animatedmenu.util.MaterialMatcher;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.Animatable;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class AnimatedMaterial extends Animatable<IPlaceholder<ItemStack>> {
    
    private static final long serialVersionUID = 3993512547684702735L;
    
    @Override
    protected IPlaceholder<ItemStack> convert(Nagger nagger, Object o) {
        final StringBundle sb = StringBundle.parse(nagger, o.toString());
        if (sb.containsPlaceholders()) {
            return (n, player) -> parseItemStack(n, sb.toString(player));
        } else return IPlaceholder.constant(parseItemStack(nagger, sb.toString(null)));
    }
    
    @Override
    protected IPlaceholder<ItemStack> defaultValue() {
        return IPlaceholder.constant(new ItemStack(Material.STONE));
    }

    public static ItemStack parseItemStack(Nagger nagger, String str) {
        String[] split = str.split(":");
        MaterialMatcher matcher = MaterialMatcher.parse(split[0]);
        if(!matcher.matches())
            nagger.nag("Couldn't find appropiate material for " + split[0] + "! Defaulting to stone");
        ItemStack item = new ItemStack(matcher.get(null, null));
        if(split.length > 1) {
            try {
                int amount = Integer.parseInt(split[1]);
                if (amount < 1) amount = 1;
                if (amount > 64) amount = 64;
                item.setAmount(amount);
            } catch (NumberFormatException ex) {
                nagger.nag("Invalid amount in " + str + "! Defaulting to 1");
            }
            if(split.length > 2) {
                try {
                    short data = Short.parseShort(split[2]);
                    if (data < 0) data = 0;
                    item.setDurability(data);
                } catch (NumberFormatException ex) {
                    nagger.nag("Invalid data value in " + str + "! Defaulting to 0");
                }
            }
        }
        return item;
    }
}
