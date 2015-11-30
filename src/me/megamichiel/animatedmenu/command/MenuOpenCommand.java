package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.MenuRegistry;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;

import org.bukkit.entity.Player;

public class MenuOpenCommand extends Command {
	
	public MenuOpenCommand(String command) {
		super(command);
	}
	
	@Override
	public void execute(Player p) {
		MenuRegistry registry = AnimatedMenuPlugin.getInstance().getMenuRegistry();
		AnimatedMenu menu = registry.getMenu(command);
		if(menu != null) registry.openMenu(p, menu);
		else AnimatedMenuPlugin.getInstance().getLogger().warning("No menu with name " + command + " found!");
	}
}
