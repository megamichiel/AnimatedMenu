package me.megamichiel.animatedmenu.command;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastCommand extends Command {
	
	public BroadcastCommand(String command) {
		super(command);
	}
	
	@Override
	public void execute(Player p) {
		Bukkit.broadcastMessage(command.replace("%p", p.getName()));
	}
}
