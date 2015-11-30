package me.megamichiel.animatedmenu.command;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleCommand extends Command {
	
	public ConsoleCommand(String command) {
		super(command);
	}
	
	@Override
	public void execute(Player p) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%p", p.getName()));
	}
}
