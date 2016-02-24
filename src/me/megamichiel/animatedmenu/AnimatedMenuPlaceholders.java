package me.megamichiel.animatedmenu;

import java.util.Map.Entry;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.megamichiel.animatedmenu.util.FormulaPlaceholder;
import me.megamichiel.animatedmenu.util.RemoteConnections.ServerInfo;
import me.megamichiel.animatedmenu.util.StringBundle;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * 
 * A class that handles some AnimatedMenu created placeholders
 *
 */
@RequiredArgsConstructor public class AnimatedMenuPlaceholders extends PlaceholderHook {
	
	public static void register(AnimatedMenuPlugin plugin)
	{
		AnimatedMenuPlaceholders instance = new AnimatedMenuPlaceholders(plugin);
		PlaceholderAPI.registerPlaceholderHook("animatedmenu", instance);
	}
	
	private final AnimatedMenuPlugin plugin;
	
	@Override
	public String onPlaceholderRequest(Player who, String arg) {
		String lower = arg.toLowerCase();
		if (lower.startsWith("motd_"))
		{
			ServerInfo info = plugin.getConnections().get(lower.substring(5));
			return info == null ? null : info.getMotd();
		}
		if (lower.startsWith("onlineplayers_"))
		{
			ServerInfo info = plugin.getConnections().get(lower.substring(14));
			return Integer.toString(info == null ? 0 : info.getOnlinePlayers());
		}
		if (lower.startsWith("maxplayers_"))
		{
			ServerInfo info = plugin.getConnections().get(lower.substring(11));
			return Integer.toString(info == null ? 0 : info.getMaxPlayers());
		}
		if (arg.startsWith("status_"))
		{
			ServerInfo info = plugin.getConnections().get(lower.substring(7));
			if (info != null)
			{
				StringBundle bundle = info.get(info.isOnline() ? "online" : "offline", who);
				if (bundle != null)
					return bundle.toString(who);
			}
			return null;
		}
		if (lower.startsWith("motdcheck_"))
		{
			ServerInfo info = plugin.getConnections().get(lower.substring(10));
			if (info != null)
			{
				String motd = info.getMotd();
				StringBundle def = null;
				for (Entry<StringBundle, StringBundle> entry : info.getValues().entrySet())
				{
					String str = entry.getKey().toString(who);
					if (str.equals(motd))
						return entry.getValue().toString(who);
					else if (str.equalsIgnoreCase("default"))
						def = entry.getValue();
				}
				if (def != null)
					return def.toString(who);
			}
			return null;
		}
		if (lower.startsWith("worldplayers_"))
		{
			String worldName = arg.substring(13);
			for (World world : Bukkit.getWorlds())
				if (world.getName().equalsIgnoreCase(worldName))
					return String.valueOf(world.getPlayers().size());
			return "0";
		}
		if (lower.startsWith("formula_"))
		{
			FormulaPlaceholder ph = plugin.getFormulaPlaceholders().get(lower.substring(8));
			if (ph != null)
				return ph.invoke(plugin, who).toString();
			return null;
		}
		return null;
	}
}
