package me.megamichiel.animatedmenu.menu;

import static me.megamichiel.animatedmenu.util.StringUtil.parseBoolean;
import static org.bukkit.ChatColor.translateAlternateColorCodes;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.command.CommandExecutor;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class DefaultClickListener implements ItemClickListener {
	
	private static final String PERMISSION_MESSAGE = "&cYou are not permitted to do that!",
			PRICE_MESSAGE = "&cYou don't have enough money for that!",
			POINTS_MESSAGE = "&cYou don't have enough points for that!";
	
	private final List<ClickProcessor> clicks = new ArrayList<>();
	
	public DefaultClickListener(AnimatedMenuPlugin plugin, ConfigurationSection section) {
		if (section.isConfigurationSection("Commands"))
		{
			ConfigurationSection commandSection = section.getConfigurationSection("Commands");
			for (String key : commandSection.getKeys(false))
			{
				if (commandSection.isConfigurationSection(key))
				{
					ConfigurationSection sec = commandSection.getConfigurationSection(key);
					CommandExecutor commandExecutor = new CommandExecutor(plugin, sec.getList("Commands")),
							buyCommandExecutor = new CommandExecutor(plugin, sec.getList("Buy-Commands"));
					String click = sec.getString("Click-Type", "both").toLowerCase();
					boolean rightClick = click.equals("both") || click.equals("right"),
							leftClick = click.equals("both") || click.equals("left");
					int shiftClick = sec.contains("Shift-Click") ? parseBoolean(sec, "Shift-Click", false) ? 2 : 1 : 0,
							price = sec.getInt("Price", -1), points = sec.getInt("Points", -1);
					String permission = sec.getString("Permission"),
							permissionMessage = color(sec.getString("Permission-Message", PERMISSION_MESSAGE)),
							bypassPermission = sec.getString("Bypass-Permission"),
							priceMessage = color(sec.getString("Price-Message", PRICE_MESSAGE)),
							pointsMessage = color(sec.getString("Points-Message", POINTS_MESSAGE));
					boolean close = parseBoolean(sec, "Close", false);
					ClickProcessor processor = new ClickProcessor(plugin, commandExecutor, buyCommandExecutor,
							rightClick, leftClick, shiftClick, price, points, permission, permissionMessage,
							bypassPermission, priceMessage, pointsMessage, close);
					clicks.add(processor);
				}
			}
		}
	}
	
	private String color(String str) {
		return str == null ? null : translateAlternateColorCodes('&', str);
	}
	
	@Override
	public void onClick(Player who, ClickType click, MenuItem item) {
		for (ClickProcessor cp : this.clicks)
			cp.onClick(who, click, item);
	}
	
	@RequiredArgsConstructor private static class ClickProcessor implements ItemClickListener
	{
		private final AnimatedMenuPlugin plugin;
		private final CommandExecutor commandExecutor, buyCommandExecutor;
		private final boolean rightClick, leftClick;
		private final int shiftClick, price, pointPrice;
		private final String permission, permissionMessage, bypassPermission, priceMessage, pointsMessage;
		private final boolean close;
		
		@Override
		public void onClick(Player who, ClickType click, MenuItem item) {
			if (((rightClick && click.isRightClick()) || (leftClick && click.isLeftClick()))
					&& (shiftClick == 0 || click.isShiftClick() == (shiftClick == 2)))
			{
				if (bypassPermission == null || !who.hasPermission(bypassPermission))
				{
					if (permission != null && !who.hasPermission(permission))
					{
						who.sendMessage(permissionMessage);
						return;
					}
					boolean bought = false;
					if (price != -1 && plugin.isVaultPresent() && !who.hasPermission("animatedmenu.economy.bypass"))
					{
						Economy econ = plugin.economy;
						if (econ.getBalance(who) >= price)
						{
							econ.withdrawPlayer(who, price);
							bought = true;
						}
						else
						{
							who.sendMessage(priceMessage);
							return;
						}
					}
					if (pointPrice != -1 && plugin.isPlayerPointsPresent()
							&& !who.hasPermission("animatedmenu.points.bypass"))
					{
						if (plugin.playerPointsAPI.take(who.getUniqueId(), pointPrice))
						{
							bought = true;
						}
						else
						{
							who.sendMessage(pointsMessage);
							return;
						}
					}
					if (bought)
						buyCommandExecutor.execute(plugin, who, click);
				}
				commandExecutor.execute(plugin, who, click);
				if (close) {
					who.closeInventory();
				}
			}
		}
	}
}
