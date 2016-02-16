package me.megamichiel.animatedmenu.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import lombok.NoArgsConstructor;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.configuration.ConfigurationSection;

@NoArgsConstructor
public abstract class Animatable<E> extends ArrayList<E> {
	
	private static final long serialVersionUID = -7324365301382371283L;
	private int frame = 0;
	protected E defaultValue;
	
	public Animatable(Collection<? extends E> c)
	{
		super(c);
	}
	
	public Animatable(E[] elements)
	{
		super(Arrays.asList(elements));
	}
	
	public E get()
	{
		return isEmpty() ? defaultValue() : get(frame);
	}
	
	public E next()
	{
		E current = get();
		if ((++frame) == size())
			frame = 0;
		return current;
	}
	
	protected E convert(Nagger nagger, String str)
	{
		return null;
	}
	
	protected E defaultValue()
	{
		return defaultValue;
	}
	
	public boolean load(AnimatedMenuPlugin plugin, ConfigurationSection section, String key)
	{
		return load(plugin, section, key, null);
	}
	
	public boolean load(AnimatedMenuPlugin plugin, ConfigurationSection section, String key, E defaultValue)
	{
		this.defaultValue = defaultValue;
		if (section.isConfigurationSection(key))
		{
			ConfigurationSection sec = section.getConfigurationSection(key);
			String str;
			for (int num = 1; (str = sec.getString(String.valueOf(num))) != null; num++, add(convert(plugin, str)));
			return true;
		}
		String value = section.getString(key);
		if (value != null)
		{
			add(convert(plugin, value));
			return true;
		}
		return false;
	}
}
