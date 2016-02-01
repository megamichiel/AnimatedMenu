package me.megamichiel.animatedmenu.util;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class StringBundle extends ArrayList<Object> {
	
	private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&([0-9a-fk-or])");
	private static final long serialVersionUID = 5196073861968616865L;
	
	private final Nagger nagger;
	
	public StringBundle(Nagger nagger, String value) {
		this.nagger = nagger;
		add(value);
	}
	
	public StringBundle(Nagger nagger) {
		this.nagger = nagger;
	}
	
	public StringBundle colorAmpersands() {
		for (int i = 0, size = size(); i < size; i++)
			if(get(i) instanceof String)
				set(i, COLOR_PATTERN.matcher((String) get(i)).replaceAll(ChatColor.COLOR_CHAR + "$1"));
		return this;
	}
	
	public String toString(Player player)
	{
		StringBuilder sb = new StringBuilder();
		for (Object o : this)
		{
			if (o instanceof Placeholder)
				sb.append(((Placeholder) o).toString(nagger, player));
			else sb.append(String.valueOf(o));
		}
		return sb.toString();
	}
	
	public static StringBundle[] fromArray(Nagger nagger, String... array) {
		StringBundle[] bundles = new StringBundle[array.length];
		for(int i = 0; i < array.length; i++)
			bundles[i] = new StringBundle(nagger, array[i]);
		return bundles;
	}
}
