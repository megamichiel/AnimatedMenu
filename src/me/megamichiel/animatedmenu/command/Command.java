package me.megamichiel.animatedmenu.command;

import org.bukkit.entity.Player;

public class Command {
	
	final String command;
	
	public Command(String command) {
		this.command = command;
	}
	
	public void execute(Player p) {
		p.performCommand(command.replace("%p", p.getName()));
	}
}
