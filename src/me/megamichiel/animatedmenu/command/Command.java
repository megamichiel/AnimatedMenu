package me.megamichiel.animatedmenu.command;

import lombok.Getter;

import org.bukkit.entity.Player;

public class Command {
	
	@Getter
	final String command;
	
	public Command(String command) {
		this.command = command;
	}
	
	public void execute(Player p) {
		p.performCommand(command.replace("%p", p.getName()));
	}
}
