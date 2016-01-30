package me.megamichiel.animatedmenu.animation;

import java.util.Collection;

import lombok.NoArgsConstructor;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.util.MaterialMatcher;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.inventory.ItemStack;

@NoArgsConstructor
public class AnimatedMaterial extends Animatable<ItemStack> {
	
	private static final long serialVersionUID = 3993512547684702735L;
	
	public AnimatedMaterial(Collection<? extends ItemStack> c) {
		super(c);
	}
	
	public AnimatedMaterial(ItemStack[] elements) {
		super(elements);
	}
	
	@Override
	protected ItemStack convert(Nagger nagger, AnimatedMenu menu, String str) {
		String[] split = str.split(":");
		MaterialMatcher matcher = MaterialMatcher.matcher(split[0]);
		if(!matcher.matches()) {
			nagger.nag("Couldn't find appropiate material for " + split[0] + "! Defaulting to stone");
		}
		ItemStack item = new ItemStack(matcher.get());
		if(split.length == 1) return item;
		try {
			item.setAmount(Integer.parseInt(split[1]));
		} catch (NumberFormatException ex) {
			nagger.nag("Invalid amount: " + str + "! Defaulting to 1");
		}
		if(split.length == 2) return item;
		try {
			item.setDurability(Short.parseShort(split[2]));
		} catch (NumberFormatException ex) {
			nagger.nag("Invalid data value: " + split[2] + "! Defaulting to 0");
		}
		return item;
	}
}
