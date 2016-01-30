package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.entity.Player;

public class MessageCommand extends Command {
	
	public MessageCommand(Nagger nagger, String command) {
		super(nagger, command);
	}
	
	@Override
	public boolean execute(AnimatedMenuPlugin plugin, Player p) {
		p.sendMessage(command.toString(p));
		return true;
	}
}
