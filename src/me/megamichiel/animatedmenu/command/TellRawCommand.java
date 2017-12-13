package me.megamichiel.animatedmenu.command;

import java.lang.reflect.Method;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TellRawCommand extends Command<StringBundle, Object> {

    private final Method deserialize, getHandle, sendMessage;
    
    public TellRawCommand() {
        super("tellraw");

        Method deserialize, getHandle, sendMessage;
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            String nms = "net.minecraft.server." + pkg.split("\\.")[3];
            try {
                deserialize = Class.forName(nms + ".IChatBaseComponent$ChatSerializer").getDeclaredMethod("a", String.class);
            } catch (ClassNotFoundException ex) {
                deserialize = Class.forName(nms + ".ChatSerializer").getDeclaredMethod("a", String.class);
            }
            Class<?> chatComponent = Class.forName(nms + ".IChatBaseComponent");
            getHandle = Class.forName(pkg + ".entity.CraftPlayer").getMethod("getHandle");
            sendMessage = getHandle.getReturnType().getMethod("sendMessage", chatComponent);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        this.deserialize = deserialize;
        this.getHandle = getHandle;
        this.sendMessage = sendMessage;
    }
    
    @Override
    public StringBundle parse(Nagger nagger, String command) {
        return StringBundle.parse(nagger, command).colorAmpersands();
    }

    @Override
    public Object tryCacheValue(AnimatedMenuPlugin plugin, StringBundle value) {
        if (!value.containsPlaceholders()) {
            String s = value.toString(null);
            try {
                return deserialize.invoke(null, s);
            } catch (Exception ex) {
                plugin.nag("Failed to parse raw message '" + s + "'");
                plugin.nag(ex);
            }
        }
        return null;
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
            sendMessage.invoke(getHandle.invoke(p), value);
        } catch (Exception ex) {
            plugin.nag("Failed to send raw message '" + value + "'!");
            plugin.nag(ex);
        }
        return true;
    }
}
