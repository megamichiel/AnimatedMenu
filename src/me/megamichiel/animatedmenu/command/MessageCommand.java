package me.megamichiel.animatedmenu.command;

import org.bukkit.entity.Player;

public class MessageCommand extends Command {
	
	public MessageCommand(String command) {
		super(command);
	}
	
	@Override
	public void execute(Player p) {
		p.sendMessage(command.replace("%p", p.getName()));
	}
}
