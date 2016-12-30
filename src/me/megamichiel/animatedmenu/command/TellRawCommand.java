package me.megamichiel.animatedmenu.command;

import java.lang.reflect.Method;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TellRawCommand extends Command<StringBundle, Object> {
    
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
    public StringBundle parse(Nagger nagger, String command) {
        return StringBundle.parse(nagger, command).colorAmpersands();
    }

    @Override
    public Object tryCacheValue(AnimatedMenuPlugin plugin, StringBundle value) {
        if (value.containsPlaceholders()) return null;
        String s = value.toString(null);
        try {
            return deserialize.invoke(null, s);
        } catch (Exception ex) {
            plugin.nag("Failed to parse raw message '" + s + "'");
            plugin.nag(ex);
            return null;
        }
    }

    @Override
    public boolean execute(AnimatedMenuPlugin plugin, Player p, StringBundle value) {
        String s = value.toString(p);
        try {
            return executeCached(plugin, p, deserialize.invoke(null, s));
        } catch (Exception ex) {
            plugin.nag("Failed to parse raw message '" + s + "'");
            plugin.nag(ex);
        }
        return true;
    }
    
    @Override
    public boolean executeCached(AnimatedMenuPlugin plugin, Player p, Object value) {
        try {
            sendMessage.invoke(getHandle.invoke(p), deserialize.invoke(null, value));
        } catch (Exception ex) {
            plugin.nag("Failed to send raw message '" + value + "'!");
            plugin.nag(ex);
        }
        return true;
    }
}
