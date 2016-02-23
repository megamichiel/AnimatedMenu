package me.megamichiel.animatedmenu.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;

public class RemoteConnections implements Runnable {
	
	private final Map<String, ServerInfo> statuses = new ConcurrentHashMap<String, ServerInfo>();
	
	@Override
	public void run() {
		for (Entry<String, ServerInfo> entry : statuses.entrySet())
		{
			try
			{
				ServerInfo info = entry.getValue();
				Socket socket = new Socket(info.address.getAddress(), info.address.getPort());
				socket.getOutputStream().write(0xFE);
				InputStream input = socket.getInputStream();
				input.read();
				int length = (input.read() << 8) | input.read();
				char[] array = new char[length];
				for (int i = 0; i < length; i++)
					array[i] = (char) ((input.read() << 8) | input.read());
				String motd = new String(array);
				motd = motd.substring(0, motd.lastIndexOf('\u00A7', motd.lastIndexOf('\u00A7') - 1));
				info.online = true;
				info.motd = motd;
				socket.close();
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
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
		private final Map<String, StringBundle> values = new HashMap<String, StringBundle>();
		private boolean online = false;
		private String motd = ChatColor.RED + "Offline";
	}
}
