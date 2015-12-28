package me.megamichiel.animatedmenu.menu;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

import java.util.ArrayList;
import java.util.List;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.command.ClickExclusiveCommandExecutor;
import me.megamichiel.animatedmenu.command.ClickExclusiveCommandExecutor.ClickExclusiveShiftCommandExecutor;
import me.megamichiel.animatedmenu.command.CommandExecutor;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class DefaultClickListener implements ItemClickListener {
	
	private final AnimatedMenuPlugin plugin = AnimatedMenuPlugin.getInstance();
	private final List<CommandExecutor> commandExecutors = new ArrayList<CommandExecutor>();
	private final CommandExecutor buyCommandExecutor;
	private final String permission, bypassPermission;
	private String permissionMessage = "&cYou are not permitted to do that!",
			priceMessage = "&cYou don't have enough money for that!",
			pointsMessage = "&cYou don't have enough points for that!";
	private final int price, pointPrice;
	private final boolean close, hidden;
	
	public DefaultClickListener(ConfigurationSection section) {
		CommandExecutor[] executors = {
				new CommandExecutor(section.getStringList("Commands")),
				new ClickExclusiveCommandExecutor(section.getStringList("Right-Click-Commands"), true),
				new ClickExclusiveCommandExecutor(section.getStringList("Left-Click-Commands"), false),
				new ClickExclusiveShiftCommandExecutor(section.getStringList("Shift-Right-Click-Commands"), true, true),
				new ClickExclusiveShiftCommandExecutor(section.getStringList("Shift-Left-Click-Commands"), false, true),
				new ClickExclusiveShiftCommandExecutor(section.getStringList("Non-Shift-Right-Click-Commands"), true, false),
				new ClickExclusiveShiftCommandExecutor(section.getStringList("Non-Shift-Left-Click-Commands"), false, false) };
		for(CommandExecutor executor : executors) {
			if(!executor.isEmpty()) {
				commandExecutors.add(executor);
			}
		}
		buyCommandExecutor = new CommandExecutor(section.getStringList("Buy-Commands"));
		permission = section.getString("Permission");
		permissionMessage = get(section.getString("Permission-Message"), permissionMessage);
		bypassPermission = section.getString("Bypass-Permission");
		price = section.getInt("Price", -1);
		pointPrice = section.getInt("Points", -1);
		priceMessage = get(section.getString("Price-Message"), priceMessage);
		pointsMessage = get(section.getString("Points-Message"), pointsMessage);
		close = section.getBoolean("Close");
		hidden = section.getBoolean("Hide");
	}
	
	private String get(String str, String def) {
		return color(str == null ? def : str);
	}
	
	private String color(String str) {
		return str == null ? null : translateAlternateColorCodes('&', str);
	}
	
	@Override
	public void onClick(Player who, ClickType click, MenuItem item) {
		if(bypassPermission == null || !who.hasPermission(bypassPermission)) {
			if(permission != null && !who.hasPermission(permission)) {
				if(!hidden) {
					who.sendMessage(permissionMessage);
				}
				return;
			}
			boolean bought = false;
			if(price != -1 && plugin.isVaultPresent() && !who.hasPermission("animatedmenu.economy.bypass")) {
				Economy econ = plugin.economy;
				if(econ.getBalance(who) >= price) {
					econ.withdrawPlayer(who, price);
					bought = true;
				} else {
					who.sendMessage(priceMessage);
					return;
				}
			}
			if(pointPrice != -1 && plugin.isPlayerPointsPresent() && !who.hasPermission("animatedmenu.points.bypass")) {
				if(plugin.playerPointsAPI.take(who.getUniqueId(), pointPrice)) {
					bought = true;
				} else {
					who.sendMessage(pointsMessage);
					return;
				}
			}
			if(bought)
				buyCommandExecutor.execute(who, click);
		}
		for(CommandExecutor executor : commandExecutors)
			executor.execute(who, click);
		if(close) {
			who.closeInventory();
		}
	}
}
