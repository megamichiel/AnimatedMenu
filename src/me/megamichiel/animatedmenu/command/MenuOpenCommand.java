package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.MenuRegistry;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.entity.Player;

public class MenuOpenCommand extends Command {
	
	public MenuOpenCommand(Nagger nagger, String command) {
		super(nagger, command);
	}
	
	@Override
	public boolean execute(AnimatedMenuPlugin plugin, Player p) {
		MenuRegistry registry = plugin.getMenuRegistry();
		AnimatedMenu menu = registry.getMenu(command.toString(p));
		if(menu != null) registry.openMenu(p, menu);
		else plugin.nag("No menu with name " + command + " found!");
		return true;
	}
}
