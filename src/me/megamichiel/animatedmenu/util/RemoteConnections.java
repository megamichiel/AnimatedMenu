package me.megamichiel.animatedmenu.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.entity.Player;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RequiredArgsConstructor
public class RemoteConnections implements Runnable {
	
	private final AnimatedMenuPlugin plugin;
	private final Map<String, ServerInfo> statuses = new ConcurrentHashMap<String, ServerInfo>();
	
	@Override
	public void run() {
		for (Entry<String, ServerInfo> entry : statuses.entrySet())
		{
			ServerInfo info = entry.getValue();
			try
			{
				Socket socket = new Socket(info.address.getAddress(), info.address.getPort());
				OutputStream output = socket.getOutputStream();
				InputStream input = socket.getInputStream();
				
				// Handshake
				ByteArrayOutputStream handshake = new ByteArrayOutputStream();
				writeVarInt(handshake, 0);
				writeVarInt(handshake, 47);
				String address = info.address.getHostString();
				byte[] bytes = address.getBytes(Charsets.UTF_8);
				writeVarInt(handshake, bytes.length);
				handshake.write(bytes);
				int port = info.address.getPort();
				handshake.write((port >> 8) & 0xFF);
				handshake.write(port & 0xFF);
				writeVarInt(handshake, 1);
				writeVarInt(output, handshake.size());
				output.write(handshake.toByteArray());
				
				// Request
				ByteArrayOutputStream request = new ByteArrayOutputStream();
				writeVarInt(request, 0);
				writeVarInt(output, request.size());
				output.write(request.toByteArray());
				
				// Response
				readVarInt(input);
				if (readVarInt(input) != 0)
				{
					socket.close();
					continue;
				}
				byte[] jsonBytes = new byte[readVarInt(input)];
				input.read(jsonBytes);
				JsonElement response = new JsonParser().parse(new String(jsonBytes, Charsets.UTF_8));
				String motd;
				int online = 0, max = 0;
				if (response.isJsonObject())
				{
					JsonObject obj = response.getAsJsonObject();
					JsonElement desc = obj.get("description");
					if (desc.isJsonObject())
						motd = desc.getAsJsonObject().get("text").getAsString();
					else motd = desc.getAsString();
					JsonElement players = obj.get("players");
					if (desc.isJsonObject())
					{
						JsonObject playersObj = players.getAsJsonObject();
						online = playersObj.get("online").getAsInt();
						max = playersObj.get("max").getAsInt();
					}
				}
				else motd = response.getAsString();
				
				info.online = true;
				info.motd = motd;
				info.onlinePlayers = online;
				info.maxPlayers = max;
				
				ByteArrayOutputStream ping = new ByteArrayOutputStream();
				writeVarInt(ping, 1);
				long time = System.currentTimeMillis();
				byte[] timeBytes = new byte[8];
				for (int i = 7; i >= 0; i--)
					timeBytes[i] = (byte) ((time >> (i * 8)) & 0xFF);
				ping.write(timeBytes);
				writeVarInt(output, ping.size());
				output.write(ping.toByteArray());
				
				readVarInt(input);
				readVarInt(input);
				input.read(new byte[8]);
				
				socket.close();
			}
			catch (IOException ex)
			{
				if (plugin.warnOfflineServers())
				{
					plugin.nag("Failed to connect to " + info.address.getHostName() + ":" + info.address.getPort() + "!");
					plugin.nag(ex);
				}
			}
		}
	}
	
	private void writeVarInt(OutputStream output, int i) throws IOException
	{
		while ((i & -128) != 0) {
			output.write(i & 127 | 128);
			i >>>= 7;
		}
		
		output.write(i);
	}
	
	private int readVarInt(InputStream stream) throws IOException
	{
		int i = 0;
		int j = 0;

		int b0;

		do {
			b0 = stream.read();
			i |= (b0 & 127) << j++ * 7;
			if (j > 5) {
				throw new RuntimeException("VarInt too big");
			}
		} while ((b0 & 128) == 128);

		return i;
	}
	
	public void clear()
	{
		statuses.clear();
	}
	
	public ServerInfo add(String name, InetSocketAddress address)
	{
		ServerInfo info = new ServerInfo(address);
		statuses.put(name, info);
		return info;
	}
	
	public ServerInfo get(String name)
	{
		return statuses.get(name);
	}
	
	@Getter @RequiredArgsConstructor
	public class ServerInfo {
		
		private final InetSocketAddress address;
		private final Map<StringBundle, StringBundle> values = new HashMap<StringBundle, StringBundle>();
		private boolean online = false;
		private String motd = ChatColor.RED + "Offline";
		private int onlinePlayers = 0, maxPlayers = 0;
		
		public StringBundle get(String key, Player who)
		{
			for (Entry<StringBundle, StringBundle> entry : values.entrySet())
				if (entry.getKey().toString(who).equals(key))
					return entry.getValue();
			return null;
		}
	}
}
