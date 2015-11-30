package me.megamichiel.animatedmenu.command;

import org.bukkit.entity.Player;

public class OpCommand extends Command {
	
	public OpCommand(String command) {
		super(command);
	}
	
	@Override
	public void execute(Player p) {
		boolean op = p.isOp();
		p.setOp(true);
		super.execute(p);
		p.setOp(op);
	}
}
