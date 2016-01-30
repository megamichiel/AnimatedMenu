package me.megamichiel.animatedmenu.util;

import java.util.Map.Entry;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;

import org.bukkit.entity.Player;

public class Placeholder {
	
	private static final boolean placeHolder;
	
	static
	{
		boolean flag = false;
		try
		{
			Class.forName("me.clip.placeholderapi.PlaceholderAPI");
			flag = true;
		}
		catch (Exception ex) {}
		placeHolder = flag;
	}
	
	private final String plugin;
	private final String name;
	private PlaceholderHook handle;
	private boolean notified = false;
	
	Placeholder(String name)
	{
		int index = name.indexOf('_');
		if (index > 0)
		{
			this.plugin = name.substring(0, index);
			this.name = name.substring(index + 1);
		}
		else
		{
			plugin = "";
			this.name = name;
		}
	}
	
	@Override
	public String toString() {
		return "{" + plugin + "_" + name + "}";
	}
	
	public String toString(Nagger nagger, Player p)
	{
		if (!placeHolder) return toString();
		if (handle == null)
		{
			for (Entry<String, PlaceholderHook> entry : PlaceholderAPI.getPlaceholders().entrySet())
			{
				if (entry.getKey().equalsIgnoreCase(this.plugin))
				{
					handle = entry.getValue();
					break;
				}
			}
		}
		if (handle != null)
		{
			String str = handle.onPlaceholderRequest(p, name);
			if (str != null)
				return str;
		}
		else if (!notified)
		{
			nagger.nag("Couldn't find placeholder by ID \"" + plugin + "_" + name + "\"!");
			notified = true;
		}
		return toString();
	}
}
