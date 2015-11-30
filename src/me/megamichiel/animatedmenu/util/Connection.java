package me.megamichiel.animatedmenu.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;


public class Connection {
	
	private static List<String> offline = new ArrayList<String>();
	
	private static boolean mv;
	
	public static boolean init() {
		return mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null;
	}
	
	/* Non-Static */
	
	private String ip;
	private int port;
	private boolean checkOnline;
	
	@Getter
	private int onlinePlayers, maxPlayers;
	@Getter
	private String motd;
	@Getter
	private boolean online;
 
	public Connection(String ip, int port, boolean checkOnline) {
		this.ip = ip;
		this.port = port;
		this.checkOnline = checkOnline;
	}
	
	public void connect() {
		if(offline.contains(address())) return;
		new BukkitRunnable() {
			Socket sock;
			DataOutputStream out;
			DataInputStream in;
			
			@Override
			public void run() {
				try {
					sock = new Socket(ip, port);
					out = new DataOutputStream(sock.getOutputStream());
					in = new DataInputStream(sock.getInputStream());
					out.write(0xFE);
				 
					int b;
					StringBuffer str = new StringBuffer();
					while ((b = in.read()) != -1) {
						if (b != 0 && b > 16 && b != 255 && b != 23 && b != 24) {
							str.append((char) b);
						}
					}
					if(checkOnline) {
						online = true;
						return;
					}
					String[] data = str.toString().split("§");
					motd = data[0];
					try {
						onlinePlayers = Integer.parseInt(data[1]);
					} catch (NumberFormatException e) {
						onlinePlayers = 0;
					} try {
						maxPlayers = Integer.parseInt(data[2]);
					} catch (NumberFormatException e) {
						maxPlayers = 0;
					}
				} catch (ConnectException ex) {
					if(checkOnline) {
						online = false;
						return;
					}
					AnimatedMenuPlugin pl = AnimatedMenuPlugin.getInstance();
					onlinePlayers = 0;
					maxPlayers = 0;
					motd = "";
					pl.getLogger().warning("Connection timed out whilst trying to connect to '" + address() + "'!");
					offline.add(address());
				} catch (UnknownHostException ex) {
					if(checkOnline) {
						online = false;
						return;
					}
					AnimatedMenuPlugin.getInstance().getLogger().warning("&cYou are trying to connect to an unknown host: " + address());
				} catch (IOException ex) {
					if(checkOnline) {
						online = false;
						return;
					}
					ex.printStackTrace();
				} finally {
					try {
						in.close();
						out.close();
						sock.close();
					} catch (Exception ex) {}
				}
			}
		}.runTaskAsynchronously(AnimatedMenuPlugin.getInstance());
	}
	
	private String address() {
		return ip + ":" + port;
	}
	
	public static class BungeeConnection implements PluginMessageListener {
		
		private String serverName;
		private static Map<String, Integer> online = new HashMap<String, Integer>();
		
		public BungeeConnection() {}
		
		public BungeeConnection(Player forPlayer, String serverName) {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);
			try {
				out.writeUTF("PlayerCount");
				out.writeUTF(serverName);
			} catch (IOException e) {
				e.printStackTrace();
			}
			forPlayer.sendPluginMessage(AnimatedMenuPlugin.getInstance(), "BungeeCord", b.toByteArray());
			this.serverName = serverName;
		}
		
		@Override
		public void onPluginMessageReceived(String channel, Player player, byte[] message) {
			if(!channel.equalsIgnoreCase("BungeeCord")) return;
			try {
				DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
				String subChannel = in.readUTF();
				if(subChannel.equalsIgnoreCase("PlayerCount")) {
					String serverName = in.readUTF();
					int count = in.readInt();
					online.put(serverName, count);
				}
			} catch (Exception ex) { //Can't catch an EOFException normally :/
				if(!(ex instanceof EOFException)) ex.printStackTrace();
			}
		}
		
		public int getPlayerCount() {
			return online.containsKey(serverName) ? online.get(serverName) : 0;
		}
	}
	
	public static class MultiverseInfo {
		
		private MultiverseWorld mvWorld;
		private World world;
		private boolean exists = true;
		
		public MultiverseInfo(String worldName) {
			if(!mv) {
				mvWorld = null;
				world = null;
				exists = false;
				return;
			}
			MVWorldManager m = ((MultiverseCore)Bukkit.getPluginManager().getPlugin("Multiverse-Core")).getMVWorldManager();
			mvWorld = m.getMVWorld(worldName);
			if(mvWorld != null)
				world = mvWorld.getCBWorld();
			else exists = false;
		}
		
		public boolean exists() {
			return exists;
		}
		
		public int getOnlinePlayers() {
			return exists ? world.getPlayers().size() : 0;
		}
		
		public int getMaxPlayers() {
			return exists ? mvWorld.getPlayerLimit() : 0;
		}
	}
}
