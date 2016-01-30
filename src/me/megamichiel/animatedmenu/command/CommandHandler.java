package me.megamichiel.animatedmenu.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.megamichiel.animatedmenu.util.Nagger;

@RequiredArgsConstructor @Getter
public abstract class CommandHandler {
	
	private final String prefix;
	
	public abstract Command getCommand(Nagger nagger, String command);
}
