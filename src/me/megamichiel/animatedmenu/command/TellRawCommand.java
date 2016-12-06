package me.megamichiel.animatedmenu.command;

import java.lang.reflect.Method;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TellRawCommand extends TextCommand {
    
    private final Method deserialize, getHandle, sendMessage;
    
    public TellRawCommand(Nagger nagger) {
        super("tellraw");
		Method m1 = null, m2 = null, m3 = null;
		try {
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
		} catch (Exception ex) {
			nagger.nag("Unable to load chat message class! Tellraw won't be usable!");
			nagger.nag(ex);
		}
		deserialize = m1;
		getHandle = m2;
		sendMessage = m3;
    }
    
    @Override
    public boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
        try {
            sendMessage.invoke(getHandle.invoke(p), deserialize.invoke(null, value));
        } catch (Exception ex) {
            plugin.nag("Failed to tell raw message '" + value + "'!");
            plugin.nag(ex);
        }
        return true;
    }
}
