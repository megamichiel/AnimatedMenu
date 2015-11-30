package me.megamichiel.animatedmenu.util;

import java.util.ArrayList;
import java.util.regex.Pattern;

import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.placeholder.PlaceHolder;

import org.bukkit.ChatColor;

public class StringBundle extends ArrayList<Object> {
	
	private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&([0-9a-fk-or])");
	private static final long serialVersionUID = 5196073861968616865L;
	
	public StringBundle(String value) {
		super();
		add(value);
	}
	
	public StringBundle() {
		super();
	}
	
	public StringBundle colorAmpersands() {
		for(int i=0;i<size();i++)
			if(get(i) instanceof String)
				set(i, COLOR_PATTERN.matcher((String) get(i)).replaceAll(ChatColor.COLOR_CHAR + "$1"));
		return this;
	}
	
	public StringBundle loadPlaceHolders(AnimatedMenu menu) {
		for(Object o : this) {
			if(o instanceof PlaceHolder) {
				PlaceHolder placeHolder = (PlaceHolder) o;
				placeHolder.init(menu);
			}
		}
		return this;
	}
	
	@Override
	public String toString() {
		return StringUtil.join(this, "", false);
	}
	
	public static StringBundle[] fromArray(String... array) {
		StringBundle[] bundles = new StringBundle[array.length];
		for(int i = 0; i < array.length; i++)
			bundles[i] = new StringBundle(array[i]);
		return bundles;
	}
}
