package me.megamichiel.animatedmenu.command;

import java.util.List;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class CommandExecutor {
	
	private final Command command;
	
	public CommandExecutor(AnimatedMenuPlugin plugin, List<?> commands) {
		this.command = parseCommands(plugin, commands);
	}
	
	public boolean isEmpty() {
		return command == null;
	}
	
	public void execute(AnimatedMenuPlugin plugin, Player p, ClickType click) {
		for (Command command = this.command; command != null; command = command.next)
			command.execute(plugin, p);
	}
	
	public static boolean isPrimitiveWrapper(Object input) {
		return input instanceof Integer || input instanceof Boolean
				|| input instanceof Character || input instanceof Byte
				|| input instanceof Short || input instanceof Double
				|| input instanceof Long || input instanceof Float;
	}
	
	private Command parseCommands(AnimatedMenuPlugin plugin, List<?> commands) {
		if (commands == null) return null;
		Command first = null, next = null;
		for(Object o : commands) {
			if (o instanceof String || isPrimitiveWrapper(o))
			{
				String str = ChatColor.translateAlternateColorCodes('&', String.valueOf(o));
				Command cmd = null;
				for(CommandHandler commandHandler : plugin.getCommandHandlers()) {
					if(str.toLowerCase().startsWith(commandHandler.getPrefix().toLowerCase() + ":")) {
						cmd = commandHandler.getCommand(plugin,
								str.substring(commandHandler.getPrefix().length() + 1).trim());
						break;
					}
				}
				if(cmd == null) {
					cmd = new Command(plugin, str);
				}
				if (first == null)
					first = next = cmd;
				else
				{
					next.next = cmd;
					next = cmd;
				}
			}
		}
		return first;
	}
}
