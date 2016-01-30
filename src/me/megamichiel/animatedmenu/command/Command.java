package me.megamichiel.animatedmenu.command;

import lombok.Getter;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Nagger;
import me.megamichiel.animatedmenu.util.StringBundle;
import me.megamichiel.animatedmenu.util.StringUtil;

import org.bukkit.entity.Player;

public class Command {
	
	@Getter
	protected final StringBundle command;
	protected Command next;
	
	public Command(Nagger nagger, String command) {
		this.command = StringUtil.parseBundle(nagger, command);
	}
	
	public boolean execute(AnimatedMenuPlugin plugin, Player p) {
		p.performCommand(command.toString(p));
		return true;
	}
}
