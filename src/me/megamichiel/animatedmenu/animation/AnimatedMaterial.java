package me.megamichiel.animatedmenu.animation;

import java.util.Collection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import me.megamichiel.animatedmenu.animation.AnimatedMaterial.Frame;
import me.megamichiel.animatedmenu.util.MaterialMatcher;
import me.megamichiel.animatedmenu.util.Nagger;
import me.megamichiel.animatedmenu.util.NumberPlaceholder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@NoArgsConstructor
public class AnimatedMaterial extends Animatable<Frame> {
	
	private static final long serialVersionUID = 3993512547684702735L;
	
	public AnimatedMaterial(Collection<? extends Frame> c) {
		super(c);
	}
	
	public AnimatedMaterial(Frame[] elements) {
		super(elements);
	}
	
	@Override
	protected Frame convert(Nagger nagger, String str) {
		return new Frame(nagger, str);
	}
	
	@Override
	protected Frame defaultValue() {
		return new Frame(Material.STONE);
	}
	
	@RequiredArgsConstructor @Getter public static class Frame 
	{
		private final MaterialMatcher type;
		private final NumberPlaceholder amount, data;
		
		public Frame(Material type) {
			this.type = new MaterialMatcher(type);
			amount = NumberPlaceholder.of(1);
			data = NumberPlaceholder.ZERO;
		}
		
		public Frame(Nagger nagger, String str) {
			String[] split = str.split(":");
			MaterialMatcher matcher = MaterialMatcher.dynamic(split[0]);
			if(!matcher.matches()) {
				nagger.nag("Couldn't find appropiate material for " + split[0] + "! Defaulting to stone");
			}
			NumberPlaceholder amount = NumberPlaceholder.of(1), data = NumberPlaceholder.ZERO;
			if (split.length > 1)
			{
				try
				{
					amount = new NumberPlaceholder(split[1]);
				}
				catch (IllegalArgumentException ex) {
					nagger.nag("Invalid amount: " + str + "! Defaulting to 1");
				}
				if (split.length > 2)
				{
					try
					{
						data = new NumberPlaceholder(split[2]);
					}
					catch (IllegalArgumentException ex)
					{
						nagger.nag("Illegal data value: " + str + "! Defaulting to 0");
					}
				}
			}
			this.type = matcher;
			this.amount = amount;
			this.data = data;
		}
		
		public ItemStack toItemStack(Nagger nagger, Player who)
		{
			return new ItemStack(type.get(nagger, who), amount.invoke(nagger, who), data.invoke(nagger, who).shortValue());
		}
	}
}
