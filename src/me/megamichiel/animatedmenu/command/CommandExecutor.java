package me.megamichiel.animatedmenu.command;

import java.util.ArrayList;
import java.util.List;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class CommandExecutor {
	
	private final List<Command> commands;
	
	public CommandExecutor(List<String> commands) {
		this.commands = parseCommands(commands);
	}
	
	public boolean isEmpty() {
		return commands.isEmpty();
	}
	
	public void execute(Player p, ClickType click) {
		for(Command command : commands)
			command.execute(p);
	}
	
	private static List<Command> parseCommands(List<String> commands) {
		List<Command> list = new ArrayList<Command>();
		for(String str : commands) {
			str = ChatColor.translateAlternateColorCodes('&', str);
			boolean handled = false;
			for(CommandHandler commandHandler : AnimatedMenuPlugin.getInstance().getCommandHandlers()) {
				if(str.toLowerCase().startsWith(commandHandler.getPrefix().toLowerCase() + ":")) {
					list.add(commandHandler.getCommand(str.substring(commandHandler.getPrefix().length() + 1).trim()));
					handled = true;
					break;
				}
			}
			if(!handled) {
				list.add(new Command(str));
			}
		}
		return list;
	}
}
