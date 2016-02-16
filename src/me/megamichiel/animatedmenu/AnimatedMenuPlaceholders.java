package me.megamichiel.animatedmenu;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.megamichiel.animatedmenu.util.StringBundle;
import me.megamichiel.animatedmenu.util.StringUtil;

import org.bukkit.entity.Player;

/**
 * 
 * A class that handles some AnimatedMenu created placeholders
 *
 */
@RequiredArgsConstructor public class AnimatedMenuPlaceholders extends PlaceholderHook implements Runnable {
	
	public static void register(AnimatedMenuPlugin plugin)
	{
		AnimatedMenuPlaceholders instance = new AnimatedMenuPlaceholders(plugin);
		PlaceholderAPI.registerPlaceholderHook("animatedmenu", instance);
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, instance, 10L, 10L);
	}
	
	private final AnimatedMenuPlugin plugin;
	private final Map<String, InetSocketAddress> addresses = new ConcurrentHashMap<>();
	private final Map<String, String> motds = new ConcurrentHashMap<>();
	private final Map<String, StatusRequest> statuses = new ConcurrentHashMap<>();
	
	@Override
	public String onPlaceholderRequest(Player who, String arg) {
		arg = arg.toLowerCase();
		if (arg.startsWith("motd_"))
		{
			String motd = motds.get(arg);
			if (motd != null)
				return motd;
			String server = arg.substring(5);
			int port = 25565, index = server.indexOf('_');
			if (index > -1)
			{
				try {
					port = Integer.parseInt(server.substring(index + 1));
				} catch (NumberFormatException e) {}
				server = server.substring(0, index);
			}
			addresses.put(arg, new InetSocketAddress(server, port));
			return "";
		}
		if (arg.startsWith("status_"))
		{
			StatusRequest request = statuses.get(arg);
			if (request != null)
				return (request.online ? request.onlineString : request.offlineString).toString(who);
			String server = arg.substring(5);
			int port = 25565, index = server.indexOf('_');
			if (index > -1)
			{
				String portString = server.substring(index + 1);
				int index2 = portString.indexOf('_');
				try {
					port = Integer.parseInt(server.substring(index + 1, index2 == -1 ? server.length() : index2));
				} catch (NumberFormatException e) {}
				String online = "&aOnline", offline = "&cOffline";
				if (index2 > -1)
				{
					String str = portString.substring(index2);
					String[] split = str.split(":");
					online = split[0];
					if (split.length > 1)
						offline = split[1];
				}
				server = server.substring(0, index);
				statuses.put(arg, request = new StatusRequest(StringUtil.parseBundle(plugin, online).colorAmpersands(),
						StringUtil.parseBundle(plugin, offline).colorAmpersands()));
			}
			else request = new StatusRequest(StringUtil.parseBundle(plugin, "&aOnline").colorAmpersands(),
					StringUtil.parseBundle(plugin, "&cOffline").colorAmpersands());
			addresses.put(arg, new InetSocketAddress(server, port));
			return (request.online ? request.onlineString : request.offlineString).toString(who);
		}
		return null;
	}
	
	@Override
	public void run() {
		for (Entry<String, InetSocketAddress> entry : addresses.entrySet())
		{
			try
			{
				Socket socket = new Socket(entry.getValue().getAddress(), entry.getValue().getPort());
				socket.getOutputStream().write(0xFE);
				InputStream input = socket.getInputStream();
				input.read();
				int length = (input.read() << 8) | input.read();
				char[] array = new char[length];
				for (int i = 0; i < length; i++)
					array[i] = (char) ((input.read() << 8) | input.read());
				String val = new String(array);
				val = (val = val.substring(0, val.lastIndexOf('\u00A7'))).substring(0, val.lastIndexOf('\u00A7'));
				motds.put(entry.getKey(), val);
				StatusRequest status = statuses.get(entry.getKey());
				if (status != null)
					status.online = true;
				socket.close();
			}
			catch (IOException ex)
			{
				StatusRequest status = statuses.get(entry.getKey());
				if (status != null)
					status.online = false;
			}
		}
	}
	
	@RequiredArgsConstructor private static class StatusRequest
	{
		private final StringBundle onlineString, offlineString;
		private boolean online = false;
	}
}
