package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastCommand extends Command {
	
	public BroadcastCommand(Nagger nagger, String command) {
		super(nagger, command);
	}
	
	@Override
	public boolean execute(AnimatedMenuPlugin plugin, Player p) {
		Bukkit.broadcastMessage(command.toString(p));
		return true;
	}
}
