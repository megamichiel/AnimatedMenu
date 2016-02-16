package me.megamichiel.animatedmenu.util;

import lombok.RequiredArgsConstructor;

import org.bukkit.entity.Player;

@RequiredArgsConstructor public final class ConstantPlaceholder<T> implements IPlaceholder<T> {
	
	private final T value;
	
	@Override
	public T invoke(Nagger nagger, Player who) {
		return value;
	}
}
