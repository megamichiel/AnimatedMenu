package me.megamichiel.animatedmenu.util;

import org.bukkit.entity.Player;

public interface IPlaceholder<T> {
	
	T invoke(Nagger nagger, Player who);
}
