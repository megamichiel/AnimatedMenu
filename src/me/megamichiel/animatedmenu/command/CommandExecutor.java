package me.megamichiel.animatedmenu.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class CommandExecutor {
	
	private final List<Command> commands;
	
	public CommandExecutor(List<String> commands) {
		this.commands = parseCommands(commands);
	}
	
	public void execute(Player p, ClickType click) {
		for(Command command : commands)
			command.execute(p);
	}
	
	private static List<Command> parseCommands(List<String> commands) {
		List<Command> list = new ArrayList<Command>();
		for(String str : commands) {
			str = ChatColor.translateAlternateColorCodes('&', str);
			if(str.startsWith("console:")) {
				list.add(new ConsoleCommand(str.substring("console:".length()).trim()));
			} else if(str.startsWith("message:")) {
				list.add(new MessageCommand(str.substring("message:".length()).trim()));
			} else if(str.startsWith("op:")) {
				list.add(new OpCommand(str.substring("op:".length()).trim()));
			} else if(str.startsWith("broadcast:")) {
				list.add(new BroadcastCommand(str.substring("op:".length()).trim()));
			} else if(str.startsWith("server:")) {
				list.add(new ServerCommand(str.substring("server:".length()).trim()));
			} else if(str.startsWith("menu:")) {
				list.add(new MenuOpenCommand(str.substring("menu:".length()).trim()));
			}
			else {
				list.add(new Command(str));
			}
		}
		return list;
	}
}
