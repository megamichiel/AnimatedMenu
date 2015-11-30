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
		frame++;
		if(frame == size())
			frame = 0;
		return current;
	}
	
	abstract E convert(AnimatedMenu menu, String str);
	
	public void load(AnimatedMenu menu, ConfigurationSection section) {
		int num = 1;
		while(true) {
			String str = section.getString(String.valueOf(num));
			if(str == null) break;
			add(convert(menu, str));
			num++;
		}
	}
}
