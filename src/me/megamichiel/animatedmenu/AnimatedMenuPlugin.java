package me.megamichiel.animatedmenu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import me.megamichiel.animatedmenu.command.BroadcastCommand;
import me.megamichiel.animatedmenu.command.BungeeCommand;
import me.megamichiel.animatedmenu.command.Command;
import me.megamichiel.animatedmenu.command.CommandHandler;
import me.megamichiel.animatedmenu.command.ConsoleCommand;
import me.megamichiel.animatedmenu.command.MenuOpenCommand;
import me.megamichiel.animatedmenu.command.MessageCommand;
import me.megamichiel.animatedmenu.command.OpCommand;
import me.megamichiel.animatedmenu.command.ServerCommand;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.placeholder.PlaceHolder;
import me.megamichiel.animatedmenu.placeholder.PlaceHolderInfo;
import me.megamichiel.animatedmenu.placeholder.PlayerInfoPlaceHolderInfo;
import me.megamichiel.animatedmenu.util.Connection;
import me.megamichiel.animatedmenu.util.Connection.BungeeConnection;
import me.megamichiel.animatedmenu.util.Connection.MultiverseInfo;
import me.megamichiel.animatedmenu.util.StringBundle;
import net.milkbowl.vault.economy.Economy;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class AnimatedMenuPlugin extends JavaPlugin implements Listener {
	
	@Getter
	private static AnimatedMenuPlugin instance;
	
	@Getter
	private final List<PlaceHolderInfo> placeHolders = new ArrayList<>();
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
	public void onEnable() {
		instance = this;
		
		/* Listeners */
		Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeConnection());
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		Bukkit.getPluginManager().registerEvents(this, this);
		getCommand("animatedmenu").setExecutor(new AnimatedMenuCommand(this));
		
		/* Config / API */
		Connection.init();
		registerDefaultPlaceHolders();
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
		try {
			Class.forName("com.comphenix.protocol.ProtocolLibrary");
			MenuPacketListener.init(this); //NoClassDefFoundErrors if I create an instance from here
		} catch (Exception ex) {}
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
	public void onDisable() {
		instance = null;
		placeHolders.clear();
		for (Player p : menuRegistry.getOpenMenu().keySet()) {
			p.closeInventory();
		}
	}
	
	private void checkForUpdate() {
		new BukkitRunnable() {
			@Override
			public void run() {
				String current = getDescription().getVersion();
				try {
					URLConnection connection = new URL("https://github.com/megamichiel/AnimatedMenu").openConnection();
					Scanner scanner = new Scanner(connection.getInputStream());
					scanner.useDelimiter("\\Z");
					String content = scanner.next();
					Matcher matcher = Pattern.compile("Current\\sVersion:\\s(<b>)?([0-9]\\.[0-9]\\.[0-9])(</b>)?").matcher(content);
					matcher.find();
					String version = matcher.group(2);
					update = current.equals(version) ? null : version;
					scanner.close();
				} catch (Exception ex) {
					getLogger().warning("Failed to check for updates: " + ex.getMessage());
				}
				if(update != null) {
					getLogger().info("A new version is available! (Current version: " + current + ", new version: " + update + ")");
				}
			}
		}.runTaskAsynchronously(this);
	}
	
	private void registerDefaultPlaceHolders() {
		placeHolders.clear();
		placeHolders.add(new PlaceHolderInfo("ping-online", "0", true) {
			
			@Override
			public boolean init(PlaceHolder placeHolder, String arg) {
				List<Connection> connections = new ArrayList<>();
				String[] hosts = arg.split(",");
				for(String host : hosts) {
					String[] split = host.split(":");
					try {
						Connection con = new Connection(split[0], split.length == 2 ? Integer.parseInt(split[1]) : 25565, false);
						connections.add(con);
					} catch (NumberFormatException ex) {
						System.err.println("Invalid port! (At " + arg + ")");
						return false;
					}
				}
				if(!connections.isEmpty()) {
					placeHolder.addData(connections);
					return true;
				}
				return false;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public void postInit(PlaceHolder placeHolder, AnimatedMenu menu) {
				menu.getConnections().addAll(placeHolder.getData(0, List.class));
			}
			
			@Override
			public String getReplacement(PlaceHolder placeHolder) {
				List<Connection> connections = placeHolder.getData(0);
				int sum = 0;
				for(Connection con : connections) {
					sum += con.getOnlinePlayers();
				}
				return String.valueOf(sum);
			}
		});
		placeHolders.add(new PlaceHolderInfo("ping-max", "0", true) {
			
			@Override
			public boolean init(PlaceHolder placeHolder, String arg) {
				List<Connection> connections = new ArrayList<>();
				String[] hosts = arg.split(",");
				for(String host : hosts) {
					String[] split = host.split(":");
					try {
						Connection con = new Connection(split[0], split.length == 2 ? Integer.parseInt(split[1]) : 25565, false);
						connections.add(con);
					} catch (NumberFormatException ex) {
						System.err.println("Invalid port! (At " + arg + ")");
					}
				}
				if(!connections.isEmpty()) {
					placeHolder.addData(connections);
					return true;
				}
				return false;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public void postInit(PlaceHolder placeHolder, AnimatedMenu menu) {
				menu.getConnections().addAll(placeHolder.getData(0, List.class));
			}
			
			@Override
			public String getReplacement(PlaceHolder placeHolder) {
				List<Connection> connections = placeHolder.getData(0);
				int sum = 0;
				for(Connection con : connections) {
					sum += con.getMaxPlayers();
				}
				return String.valueOf(sum);
			}
		});
		placeHolders.add(new PlaceHolderInfo("ping-motd", "offline", true) {
			
			@Override
			public boolean init(PlaceHolder placeHolder, String arg) {
				String[] split = arg.split(":");
				try {
					Connection con = new Connection(split[0], split.length == 2 ? Integer.parseInt(split[1]) : 25565, false);
					placeHolder.addData(con);
					return true;
				} catch (NumberFormatException ex) {
					System.err.println("Invalid port! (At " + arg + ")");
					return false;
				}
			}
			
			@Override
			public void postInit(PlaceHolder placeHolder, AnimatedMenu menu) {
				menu.getConnections().add(placeHolder.getData(0, Connection.class));
			}
			
			@Override
			public String getReplacement(PlaceHolder placeHolder) {
				return placeHolder.getData(0, Connection.class).getMotd();
			}
		});
		placeHolders.add(new PlaceHolderInfo("check-online", "§cOffline", true) {
			
			@Override
			public boolean init(PlaceHolder placeHolder, String arg) {
				String[] split = arg.split(":");
				try {
					Connection con = new Connection(split[0], split.length >= 2 ? Integer.parseInt(split[1]) : 25565, false);
					placeHolder.addData(con);
					if(split.length >= 3) {
						placeHolder.addData(new StringBundle(split[split.length - 2]).colorAmpersands());
						placeHolder.addData(new StringBundle(split[split.length - 1]).colorAmpersands());
					} else {
						placeHolder.addData(new StringBundle("§aOnline"));
						placeHolder.addData(new StringBundle("§cOffline"));
					}
				} catch (NumberFormatException ex) {
					System.err.println("Invalid port! (At " + arg + ")");
					return false;
				}
				return true;
			}
			
			@Override
			public void postInit(PlaceHolder placeHolder, AnimatedMenu menu) {
				menu.getConnections().add(placeHolder.getData(0, Connection.class));
			}
			
			@Override
			public String getReplacement(PlaceHolder placeHolder) {
				return placeHolder.getData(placeHolder.getData(0, Connection.class).isOnline() ? 1 : 2, StringBundle.class).toString();
			}
		});
		placeHolders.add(new PlaceHolderInfo("bungee-online", "0", true) {
			
			@Override
			public boolean init(PlaceHolder placeHolder, String arg) {
				List<String> servers = Arrays.asList(arg.split(","));
				if(!servers.isEmpty()) {
					placeHolder.addData(servers);
					return true;
				}
				return false;
			}
			
			@Override
			public String getReplacement(PlaceHolder placeHolder) {
				List<String> servers = placeHolder.getData(0);
				Player[] online = getOnlinePlayers();
				Player target;
				if(online.length > 0) {
					target = online[0];
				} else return "0";
				int sum = 0;
				for(String server : servers) {
					BungeeConnection con = new BungeeConnection(target, server);
					sum += con.getPlayerCount();
				}
				return String.valueOf(sum);
			}
		});
		placeHolders.add(new PlaceHolderInfo("mv-online", "0", true) {
			
			@Override
			public boolean init(PlaceHolder placeHolder, String arg) {
				List<MultiverseInfo> worlds = new ArrayList<>();
				String[] worldNames = arg.split(",");
				for(String worldName : worldNames) {
					MultiverseInfo info = new MultiverseInfo(worldName);
					if(info.exists())
						worlds.add(info);					
				}
				if(!worlds.isEmpty()) {
					placeHolder.addData(worlds);
					return true;
				}
				return false;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public String getReplacement(PlaceHolder placeHolder) {
				List<MultiverseInfo> worlds = placeHolder.getData(0, List.class);
				int sum = 0;
				for(MultiverseInfo world : worlds) {
					sum += world.getOnlinePlayers();
				}
				return String.valueOf(sum);
			}
		});
		placeHolders.add(new PlaceHolderInfo("mv-max", "0", true) {
			
			@Override
			public boolean init(PlaceHolder placeHolder, String arg) {
				List<MultiverseInfo> worlds = new ArrayList<>();
				String[] worldNames = arg.split(",");
				for(String worldName : worldNames) {
					MultiverseInfo info = new MultiverseInfo(worldName);
					if(info.exists())
						worlds.add(info);					
				}
				if(!worlds.isEmpty()) {
					placeHolder.addData(worlds);
					return true;
				}
				return false;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public String getReplacement(PlaceHolder placeHolder) {
				List<MultiverseInfo> worlds = placeHolder.getData(0, List.class);
				int sum = 0;
				for(MultiverseInfo world : worlds) {
					sum += world.getMaxPlayers();
				}
				return String.valueOf(sum);
			}
		});
		placeHolders.add(new PlaceHolderInfo("region", "0", true) {
			
			@Override
			public boolean init(PlaceHolder placeHolder, String arg) {
				List<World> worlds = new ArrayList<World>();
				List<ProtectedRegion> regions = new ArrayList<ProtectedRegion>();
				String[] split = arg.split(",");
				for(String str : split) {
					String[] info = str.split(":");
					if(info.length != 2) continue;
					World world = Bukkit.getWorld(info[0]);
					if(world != null) {
						RegionManager rm = WorldGuardPlugin.inst().getRegionManager(world);
						ProtectedRegion region = rm.getRegion(info[1]);
						if(region != null) {
							worlds.add(world);
							regions.add(region);
						}
					}
				}
				if(!worlds.isEmpty() && worlds.size() == regions.size()) {
					placeHolder.addData(worlds);
					placeHolder.addData(regions);
					return true;
				}
				return false;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public String getReplacement(PlaceHolder placeHolder) {
				Iterator<World> worlds = (placeHolder.getData(0, List.class)).iterator();
				List<ProtectedRegion> regions = placeHolder.getData(0, List.class);
				int sum = 0;
				for(ProtectedRegion region : regions) {
					for(Player o : worlds.next().getPlayers()) {
						Location loc = o.getLocation();
						if(region.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
							sum++;
					}
				}
				return String.valueOf(sum);
			}
		});
		placeHolders.add(new PlayerInfoPlaceHolderInfo("name") {
			@Override
			public String getReplacement(Player p) {
				return p.getName();
			}
		});
		placeHolders.add(new PlayerInfoPlaceHolderInfo("uuid") {
			@Override
			public String getReplacement(Player p) {
				return p.getUniqueId().toString();
			}
		});
		placeHolders.add(new PlayerInfoPlaceHolderInfo("ping") {

			private final Method[] methods = new Method[1];
			private final Field[] fields = new Field[1];
			
			{
				String pkg = Bukkit.getServer().getClass().getPackage().getName();
				pkg = pkg.substring(pkg.lastIndexOf('.') + 1);
				try {
					methods[0] = Class.forName("org.bukkit.craftbukkit." + pkg + ".entity.CraftPlayer").getMethod("getHandle");
					fields[0] = Class.forName("net.minecraft.server." + pkg + ".EntityPlayer").getField("ping");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
			@Override
			public String getReplacement(Player p) {
				try {
					return String.valueOf(fields[0].get(methods[0].invoke(p)));
				} catch (Exception ex) {
					ex.printStackTrace();
					return null;
				}
			}
		});
		placeHolders.add(new PlayerInfoPlaceHolderInfo("money") {
			@Override
			public String getReplacement(Player p) {
				if(vaultPresent)
					return String.valueOf(economy.getBalance(p));
				return "0";
			}
		});
		placeHolders.add(new PlayerInfoPlaceHolderInfo("points") {
			@Override
			public String getReplacement(Player p) {
				if(playerPointsPresent)
					return String.valueOf(playerPointsAPI.look(p.getUniqueId()));
				return "0";
			}
		});
	}
	
	private void registerDefaultCommandHandlers() {
		commandHandlers.clear();
		commandHandlers.add(new CommandHandler("console") {
			@Override
			public Command getCommand(String command) {
				return new ConsoleCommand(command);
			}
		});
		commandHandlers.add(new CommandHandler("message") {
			@Override
			public Command getCommand(String command) {
				return new MessageCommand(command);
			}
		});
		commandHandlers.add(new CommandHandler("op") {
			@Override
			public Command getCommand(String command) {
				return new OpCommand(command);
			}
		});
		commandHandlers.add(new CommandHandler("broadcast") {
			@Override
			public Command getCommand(String command) {
				return new BroadcastCommand(command);
			}
		});
		commandHandlers.add(new CommandHandler("server") {
			@Override
			public Command getCommand(String command) {
				return new ServerCommand(command);
			}
		});
		commandHandlers.add(new CommandHandler("menu") {
			@Override
			public Command getCommand(String command) {
				return new MenuOpenCommand(command);
			}
		});
		commandHandlers.add(new CommandHandler("bcmd") {
			@Override
			public Command getCommand(String command) {
				return new BungeeCommand(command, false);
			}
		});
		commandHandlers.add(new CommandHandler("bpcmd") {
			@Override
			public Command getCommand(String command) {
				return new BungeeCommand(command, true);
			}
		});
	}
	
	public List<String> applyPlayerPlaceHolders(Player p, List<String> list) {
		List<String> out = new ArrayList<>();
		for(String str : list)
			out.add(applyPlayerPlaceHolders(p, str));
		return out;
	}
	
	public String applyPlayerPlaceHolders(Player p, String str) {
		if(str == null) return str;
		for(PlaceHolderInfo info : placeHolders) {
			if(info instanceof PlayerInfoPlaceHolderInfo)
				str = str.replace("%" + info.getName() + "%", String.valueOf(((PlayerInfoPlaceHolderInfo) info).getReplacement(p)));
		}
		return str;
	}
	
	public Player[] getOnlinePlayers() {
		Object online = Bukkit.getOnlinePlayers();
		if(online instanceof Collection<?>)
			return ((Collection<?>) online).toArray(new Player[0]);
		return (Player[]) online;
	}
	
	void reload() {
		placeHolders.clear();
		registerDefaultPlaceHolders();
		registerDefaultCommandHandlers();
		Bukkit.getPluginManager().callEvent(new AnimatedMenuPreLoadEvent(this));
		menuRegistry.loadMenus();
		Bukkit.getPluginManager().callEvent(new AnimatedMenuPostLoadEvent(this));
	}
	
	/* Listeners */
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent e) {
		if(update != null && e.getPlayer().hasPermission("animatedmenu.seeupdate")) {
			e.getPlayer().sendMessage("§8[§6AnimatedMenu§8] §aA new version is available! (Current version: " + getDescription().getVersion() + ", new version: " + update + ")");
		}
		for(AnimatedMenu menu : menuRegistry) {
			if(menu.getSettings().getOpener() != null && menu.getSettings().getOpenerJoinSlot() > -1) {
				e.getPlayer().getInventory().setItem(menu.getSettings().getOpenerJoinSlot(), menu.getSettings().getOpener());
			}
		}
		/* TODO remove after testing
		MenuItem item = new MenuItem(MenuItemSettings.builder("Test")
				.material(new AnimatedMaterial(new ItemStack[] {new ItemStack(Material.STONE)}))
				.displayName(new AnimatedName(StringBundle.fromArray("§6test", "§atest")))
				.lore(new AnimatedLore(new Frame[] {new Frame(new StringBundle("§atest")),
						new Frame(new StringBundle("§6test"))}))
						.frameDelay(20).build());
		
		e.getPlayer().getInventory().setItem(0, item.getHandle());
		item.setHandle(e.getPlayer().getInventory().getItem(0));
		menuRegistry.getSingleItems().add(item);*/
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if(!e.getPlayer().hasPermission("animatedmenu.open"))
			return;
		if(e.getItem() != null) {
			for(AnimatedMenu menu : menuRegistry) {
				ItemStack item = menu.getSettings().getOpener();
				if(item != null && item.getData().equals(e.getItem().getData())) {
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
	
	private boolean compare(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		Player p = e.getPlayer();
		String cmd = e.getMessage().substring(1).split(" ")[0].toLowerCase();
		for(AnimatedMenu menu : menuRegistry)
			if(cmd.equals(menu.getSettings().getOpenCommand())) {
				menuRegistry.openMenu(p, menu);
				e.setCancelled(true);
				return;
			}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent e) {
		menuRegistry.getOpenMenu().remove(e.getPlayer());
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if(!(e.getWhoClicked() instanceof Player)) //Not sure if they can't be a player, but just for safety
			return;
		Player p = (Player) e.getWhoClicked();
		AnimatedMenu open = menuRegistry.getOpenedMenu(p);
		if(open == null) return;
		e.setCancelled(true);
		int slot = e.getRawSlot();
		InventoryView view = e.getView();
		if ((view.getTopInventory() != null) && slot >= 0 && (slot < view.getTopInventory().getSize())) {
			open.getMenuGrid().click(p, e.getClick(), e.getSlot());
		}
	}
}
