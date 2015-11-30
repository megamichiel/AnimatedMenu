package me.megamichiel.animatedmenu;

import static org.bukkit.ChatColor.RED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import me.megamichiel.animatedmenu.menu.AnimatedMenu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class AnimatedMenuCommand implements CommandExecutor, TabCompleter {
	
	private final String[] messages = {
			"§3-=§6Animated Menu - Help§3=-",
			"§a/%s open <menu>§e: Open a specific menu",
			"§a/%s item <menu>§e: Get a menu's menu opener",
			"§a/%s reload§e: Reload the plugin"
	};
	private final String[] permissions = new String[] {
			"animatedmenu.command.help",
			"animatedmenu.command.reload",
			"animatedmenu.command.open",
			"animatedmenu.command.item"
	};
	private final Map<String, Integer> consoleOnly = new HashMap<String, Integer>();
	private final Map<String, Integer> playerOnly = new HashMap<String, Integer>();
	private final AnimatedMenuPlugin plugin;
	
	public AnimatedMenuCommand(AnimatedMenuPlugin plugin) {
		this.plugin = plugin;
		consoleOnly.put("reload", 1);
		playerOnly.put("reload", 1);
		playerOnly.put("open", 2);
		playerOnly.put("item", 2);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 0) {
			if(!sender.hasPermission(permissions[0]))
				return invalid(sender, "You don't have permission for that!");
			sender.sendMessage(messages[0]);
			if(sender instanceof Player) {
				for(int i=1;i<4;i++)
					if(sender.hasPermission(permissions[i]))
						sender.sendMessage(String.format(messages[i], label));
			} else sender.sendMessage(String.format(messages[3], label));
			return true;
		}
		Map<String, Integer> valid = sender instanceof Player ? playerOnly : consoleOnly;
		Integer minArgs = valid.get(args[0].toLowerCase());
		if(minArgs == null) return invalid(sender, "Invalid subcommand, type /" + label + " for help");
		if(args.length < minArgs) return invalid(sender, "Invalid argument length, type /" + label + " for help");
		switch(args[0].toLowerCase()) {
		case "reload":
			if(!(sender.hasPermission(permissions[1])))
				return invalid(sender, "You don't have permission for that!");
			plugin.reload();
			break;
		case "open":
			if(!(sender.hasPermission(permissions[2])))
				return invalid(sender, "You don't have permission for that!");
			AnimatedMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
			if(menu == null) return invalid(sender, "Couldn't find a menu by that name!");
			plugin.getMenuRegistry().openMenu((Player) sender, menu);
			break;
		case "item":
			if(!(sender.hasPermission(permissions[3])))
				return invalid(sender, "You don't have permission for that!");
			menu = plugin.getMenuRegistry().getMenu(args[1]);
			if(menu == null) return invalid(sender, "Couldn't find a menu by that name!");
			if(menu.getSettings().getOpener() == null) return invalid(sender, "That menu doesn't have a menu opener!");
			((Player) sender).getInventory().addItem(menu.getSettings().getOpener());
			break;
		}
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		List<String> list = new ArrayList<>();
		boolean player = sender instanceof Player;
		if(args.length <= 1) {
			if(player) {
				if(sender.hasPermission(permissions[2]))
					list.add("open");
				if(sender.hasPermission(permissions[3]))
					list.add("item");
				if(sender.hasPermission(permissions[1]))
					list.add("reload");
			} else list.add("reload");
			if(args.length == 1) {
				for(Iterator<String> it = list.iterator(); it.hasNext();)
					if(!it.next().startsWith(args[0].toLowerCase()))
						it.remove();
			}
		} else if(args.length == 2) {
			if(player) {
				switch(args[0].toLowerCase()) {
				case "open": case "item":
					for(AnimatedMenu menu : plugin.getMenuRegistry()) {
						if(menu.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
							list.add(menu.getName().toLowerCase());
						}
					}
					break;
				}
			}
		}
		return list;
	}
	
	private boolean invalid(CommandSender sender, String msg) {
		sender.sendMessage(RED + msg);
		return true;
	}
}
