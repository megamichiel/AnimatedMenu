package me.megamichiel.animatedmenu.util;

public interface Nagger {
	
	void nag(String message);
	
	void nag(Throwable throwable);
}
