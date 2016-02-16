package me.megamichiel.animatedmenu.util;

import lombok.RequiredArgsConstructor;

import org.bukkit.entity.Player;


@RequiredArgsConstructor public class NumberPlaceholder implements IPlaceholder<Integer> {
	
	public static final NumberPlaceholder ZERO = of(0);
	
	public static NumberPlaceholder of(final int val)
	{
		return new NumberPlaceholder(new ConstantPlaceholder<Integer>(val));
	}
	
	private final IPlaceholder<Integer> handle;
	
	public NumberPlaceholder(String string) throws IllegalArgumentException {
		IPlaceholder<Integer> placeholder = null;
		try
		{
			final int val = Integer.parseInt(string);
			placeholder = new ConstantPlaceholder<Integer>(val);
		}
		catch (NumberFormatException ex) {}
		if (placeholder == null && string.startsWith("%") && string.endsWith("%"))
		{
			final Placeholder ph = new Placeholder(string.substring(1, string.length() - 1));
			placeholder = new IPlaceholder<Integer>() {
				@Override
				public Integer invoke(Nagger nagger, Player who) {
					String result = ph.invoke(nagger, who);
					try
					{
						return Integer.parseInt(result);
					}
					catch (NumberFormatException ex)
					{
						nagger.nag("Placeholder " + ph.toString() + " didn't return a number! Got '" + result + "' instead");
						return 0;
					}
				}
			};
		}
		if (placeholder == null)
		{
			throw new IllegalArgumentException(string);
		}
		handle = placeholder;
	}
	
	@Override
	public Integer invoke(Nagger nagger, Player who) {
		return handle.invoke(nagger, who);
	}
}
