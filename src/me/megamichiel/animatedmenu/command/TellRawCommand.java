package me.megamichiel.animatedmenu.command;

import java.lang.reflect.Method;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TellRawCommand extends Command {
	
	private static final Method deserialize, getHandle, sendMessage;
	
	static
	{
		Method m1 = null, m2 = null, m3 = null;
		try
		{
			String pkg = Bukkit.getServer().getClass().getPackage().getName();
			String nms = "net.minecraft.server." + pkg.split("\\.")[3];
			try
			{
				m1 = Class.forName(nms + ".IChatBaseComponent$ChatSerializer").getDeclaredMethod("a", String.class);
			}
			catch (Exception ex)
			{
				m1 = Class.forName(nms + ".ChatSerializer").getDeclaredMethod("a", String.class);
			}
			Class<?> chatComponent = Class.forName(nms + ".IChatBaseComponent");
			m2 = Class.forName(pkg + ".entity.CraftPlayer").getMethod("getHandle");
			m3 = m2.getReturnType().getMethod("sendMessage", chatComponent);
		}
		catch (Exception ex) {}
		deserialize = m1;
		getHandle = m2;
		sendMessage = m3;
	}
	
	public TellRawCommand(Nagger nagger, String command) {
		super(nagger, command);
	}
	
	@Override
	public boolean execute(AnimatedMenuPlugin plugin, Player p) {
		String message = command.toString(p);
		try
		{
			Object msg = deserialize.invoke(null, message);
			sendMessage.invoke(getHandle.invoke(p), msg);
		}
		catch (Exception ex)
		{
			plugin.nag("Failed to tell raw message '" + message + "'!");
			plugin.nag(ex);
		}
		return true;
	}
}
