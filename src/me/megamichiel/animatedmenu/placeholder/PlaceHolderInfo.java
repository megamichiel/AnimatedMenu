package me.megamichiel.animatedmenu.placeholder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;

@AllArgsConstructor
public abstract class PlaceHolderInfo implements Cloneable {
	
	@Getter
	private final String name, defaultValue;
	private final boolean requiresArgs;
	
	public boolean requiresArgs() {
		return requiresArgs;
	}
	
	public abstract boolean init(PlaceHolder placeHolder, String arg);
	
	public void postInit(PlaceHolder placeHolder, AnimatedMenu menu) {}
	
	public abstract String getReplacement(PlaceHolder placeHolder);
	
	@Override
	public String toString() {
		throw new UnsupportedOperationException("EWWW");
		//return getReplacement(null);
	}
}