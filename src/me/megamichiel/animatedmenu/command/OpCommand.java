package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.entity.Player;

public class OpCommand extends Command {
	
	public OpCommand(Nagger nagger, String command) {
		super(nagger, command);
	}
	
	@Override
	public boolean execute(AnimatedMenuPlugin plugin, Player p) {
		boolean op = p.isOp();
		p.setOp(true);
		super.execute(plugin, p);
		p.setOp(op);
		return true;
	}
}
