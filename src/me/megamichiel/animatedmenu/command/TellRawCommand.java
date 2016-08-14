package me.megamichiel.animatedmenu.command;

import java.lang.reflect.Method;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TellRawCommand extends TextCommand {
	
	private final Method deserialize, getHandle, sendMessage;

	{
		Method m1 = null, m2 = null, m3 = null;
		try
		{
			String pkg = Bukkit.getServer().getClass().getPackage().getName();
			String nms = "net.minecraft.server." + pkg.split("\\.")[3];
			try {
				m1 = Class.forName(nms + ".IChatBaseComponent$ChatSerializer").getDeclaredMethod("a", String.class);
			} catch (Exception ex) {
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
	
	public TellRawCommand() {
		super("tellraw");
	}
	
	@Override
	public boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
		try
		{
			Object msg = deserialize.invoke(null, value);
			sendMessage.invoke(getHandle.invoke(p), msg);
		}
		catch (Exception ex)
		{
			plugin.nag("Failed to tell raw message '" + value + "'!");
			plugin.nag(ex);
		}
		return true;
	}
}
