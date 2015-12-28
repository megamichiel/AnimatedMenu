package me.megamichiel.animatedmenu.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public abstract class CommandHandler {
	
	private final String prefix;
	
	public abstract Command getCommand(String command);
}
