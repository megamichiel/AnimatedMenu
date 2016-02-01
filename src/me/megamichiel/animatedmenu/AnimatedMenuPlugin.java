package me.megamichiel.animatedmenu;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import me.megamichiel.animatedmenu.command.BroadcastCommand;
import me.megamichiel.animatedmenu.command.Command;
import me.megamichiel.animatedmenu.command.CommandHandler;
import me.megamichiel.animatedmenu.command.ConsoleCommand;
import me.megamichiel.animatedmenu.command.MenuOpenCommand;
import me.megamichiel.animatedmenu.command.MessageCommand;
import me.megamichiel.animatedmenu.command.OpCommand;
import me.megamichiel.animatedmenu.command.ServerCommand;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.util.Nagger;
import net.milkbowl.vault.economy.Economy;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AnimatedMenuPlugin extends JavaPlugin implements Listener, Nagger {
	
	@Getter
	private final List<CommandHandler> commandHandlers = new ArrayList<>();
	@Getter
	private final MenuRegistry menuRegistry = new MenuRegistry(this);
	
	private String update;
	@Getter
	private boolean vaultPresent = false, playerPointsPresent = false;
	public Economy economy;
	public PlayerPointsAPI playerPointsAPI;
	
	@Override
	public void onEnable()
	{
		/* Listeners */
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		getCommand("animatedmenu").setExecutor(new AnimatedMenuCommand(this));
		
		/* Config / API */
		registerDefaultCommandHandlers();
		try {
			Class.forName("net.milkbowl.vault.economy.Economy");
			economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
			vaultPresent = true;
		} catch (Exception ex) {} //No Vault
		Plugin pp = Bukkit.getPluginManager().getPlugin("PlayerPoints");
		if(pp != null) {
			playerPointsAPI = ((PlayerPoints) pp).getAPI();
			playerPointsPresent = true;
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getPluginManager().callEvent(new AnimatedMenuPreLoadEvent(AnimatedMenuPlugin.this));
				menuRegistry.loadMenus();
				Bukkit.getPluginManager().callEvent(new AnimatedMenuPostLoadEvent(AnimatedMenuPlugin.this));
			}
		}.runTask(this);
		
		/* Other Stuff */
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, menuRegistry, 0, 0);
		checkForUpdate();
	}
	
	@Override
	public void onDisable()
	{
		for (Player p : new HashSet<Player>(menuRegistry.getOpenMenu().keySet())) { // InventoryCloseEvent could cause ConcurrentModificationException
			p.closeInventory();
		}
	}
	
	private void checkForUpdate()
	{
		new BukkitRunnable() {
			@Override
			public void run() {
				String current = getDescription().getVersion();
				try
				{
					URLConnection connection = new URL("https://github.com/megamichiel/AnimatedMenu").openConnection();
					Scanner scanner = new Scanner(connection.getInputStream());
					scanner.useDelimiter("\\Z");
					String content = scanner.next();
					Matcher matcher = Pattern.compile("Current\\sVersion:\\s(<b>)?([0-9]\\.[0-9]\\.[0-9])(</b>)?").matcher(content);
					matcher.find();
					String version = matcher.group(2);
					update = current.equals(version) ? null : version;
					scanner.close();
				}
				catch (Exception ex)
				{
					nag("Failed to check for updates:");
					nag(ex);
				}
				if (update != null)
				{
					getLogger().info("A new version is available! (Current version: " + current + ", new version: " + update + ")");
				}
			}
		}.runTaskAsynchronously(this);
	}
	
	protected void registerDefaultCommandHandlers()
	{
		commandHandlers.clear();
		commandHandlers.add(new CommandHandler("console") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new ConsoleCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("message") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new MessageCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("op") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new OpCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("broadcast") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new BroadcastCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("server") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new ServerCommand(nagger, command);
			}
		});
		commandHandlers.add(new CommandHandler("menu") {
			@Override
			public Command getCommand(Nagger nagger, String command) {
				return new MenuOpenCommand(nagger, command);
			}
		});
	}
	
	public Player[] getOnlinePlayers()
	{
		Object online = Bukkit.getOnlinePlayers();
		if(online instanceof Collection<?>)
			return ((Collection<?>) online).toArray(new Player[0]);
		return (Player[]) online;
	}
	
	void reload()
	{
		registerDefaultCommandHandlers();
		Bukkit.getPluginManager().callEvent(new AnimatedMenuPreLoadEvent(this));
		menuRegistry.loadMenus();
		Bukkit.getPluginManager().callEvent(new AnimatedMenuPostLoadEvent(this));
	}
	
	/* Listeners */
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		if(update != null && e.getPlayer().hasPermission("animatedmenu.seeupdate"))
		{
			e.getPlayer().sendMessage("§8[§6" + getDescription().getName() + "§8] §aA new version is available! (Current version: " + getDescription().getVersion() + ", new version: " + update + ")");
		}
		for(AnimatedMenu menu : menuRegistry)
		{
			if(menu.getSettings().getOpener() != null && menu.getSettings().getOpenerJoinSlot() > -1)
			{
				e.getPlayer().getInventory().setItem(menu.getSettings().getOpenerJoinSlot(), menu.getSettings().getOpener());
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if(!e.getPlayer().hasPermission("animatedmenu.open"))
			return;
		if(e.getItem() != null)
		{
			for(AnimatedMenu menu : menuRegistry)
			{
				ItemStack item = menu.getSettings().getOpener();
				if(item != null && item.getData().equals(e.getItem().getData()))
				{
					ItemMeta meta = e.getItem().getItemMeta();
					ItemMeta meta1 = item.getItemMeta();
					if(menu.getSettings().hasOpenerName() && !compare(meta.getDisplayName(), meta1.getDisplayName()))
						continue;
					if(menu.getSettings().hasOpenerLore() && !compare(meta.getLore(), meta1.getLore()))
						continue;
					menuRegistry.openMenu(e.getPlayer(), menu);
					e.setCancelled(true);
					return;
				}
			}
		}
	}
	
	private boolean compare(Object o1, Object o2)
	{
		return o1 == null ? o2 == null : o1.equals(o2);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e)
	{
		Player p = e.getPlayer();
		String cmd = e.getMessage().substring(1).split(" ")[0].toLowerCase();
		for(AnimatedMenu menu : menuRegistry)
		{
			if(cmd.equals(menu.getSettings().getOpenCommand()))
			{
				menuRegistry.openMenu(p, menu);
				e.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent e)
	{
		AnimatedMenu menu = menuRegistry.getOpenMenu().remove(e.getPlayer());
		if (menu != null) menu.getOpenMenu().remove(e.getPlayer());
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e)
	{
		if(!(e.getWhoClicked() instanceof Player)) //Not sure if they can't be a player, but just for safety
			return;
		Player p = (Player) e.getWhoClicked();
		AnimatedMenu open = menuRegistry.getOpenedMenu(p);
		if(open == null) return;
		e.setCancelled(true);
		int slot = e.getRawSlot();
		InventoryView view = e.getView();
		if ((view.getTopInventory() != null) && slot >= 0 && (slot < view.getTopInventory().getSize()))
		{
			open.getMenuGrid().click(p, e.getClick(), e.getSlot());
		}
	}
	
	@Override
	public void nag(String message)
	{
		getLogger().warning(message);
	}
	
	@Override
	public void nag(Throwable throwable)
	{
		getLogger().warning(throwable.getClass().getName() + ": " + throwable.getMessage());
	}
}
