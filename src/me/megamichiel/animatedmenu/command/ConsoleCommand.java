package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleCommand extends Command {
	
	public ConsoleCommand(Nagger nagger, String command) {
		super(nagger, command);
	}
	
	@Override
	public boolean execute(AnimatedMenuPlugin plugin, Player p) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.toString(p));
		return true;
	}
}
