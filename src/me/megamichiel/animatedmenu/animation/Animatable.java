package me.megamichiel.animatedmenu.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import lombok.NoArgsConstructor;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;

import org.bukkit.configuration.ConfigurationSection;

@NoArgsConstructor
public abstract class Animatable<E> extends ArrayList<E> {
	
	private static final long serialVersionUID = -7324365301382371283L;
	private int frame = 0;
	
	public Animatable(Collection<? extends E> c) {
		super(c);
	}
	
	public Animatable(E[] elements) {
		super(Arrays.asList(elements));
	}
	
	public E get() {
		return get(frame);
	}
	
	public E next() {
		E current = get();
		if ((++frame) == size())
			frame = 0;
		return current;
	}
	
	protected E convert(AnimatedMenu menu, String str)
	{
		return null;
	}
	
	public void load(AnimatedMenu menu, ConfigurationSection section) {
		String str;
		for (int num = 1; (str = section.getString(String.valueOf(num))) != null; num++, add(convert(menu, str)));
	}
}
