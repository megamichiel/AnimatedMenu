package me.megamichiel.animatedmenu.placeholder;

import org.bukkit.entity.Player;

public abstract class PlayerInfoPlaceHolderInfo extends PlaceHolderInfo {

	public PlayerInfoPlaceHolderInfo(String name) {
		super(name, "NULL", false);
	}
	
	public abstract String getReplacement(Player p);
	
	@Override
	public boolean init(PlaceHolder placeHolder, String arg) {
		return true;
	}
	
	@Override
	public String getReplacement(PlaceHolder placeHolder) {
		return '%' + getName() + '%';
	}
}
