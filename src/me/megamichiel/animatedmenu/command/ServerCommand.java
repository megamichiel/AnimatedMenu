package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class ServerCommand extends Command<StringBundle, byte[]> {

    public ServerCommand() {
        super("server");
    }

    @Override
    protected StringBundle parse(Nagger nagger, String command) {
        return StringBundle.parse(nagger, command);
    }

    @Override
    protected boolean execute(AnimatedMenuPlugin plugin, Player p, StringBundle value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bytes);
        try {
            dos.writeUTF("Connect");
            dos.writeUTF(value.toString(p));
        } catch (Exception ex) {
            plugin.nag("An error occured on trying to encode bungee server '" + value + "'");
            plugin.nag(ex);
            return true;
        }
        p.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        return true;
    }

    @Override
    protected byte[] tryCacheValue(AnimatedMenuPlugin plugin, StringBundle value) {
        if (value.containsPlaceholders()) return null;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bytes);
        try {
            dos.writeUTF("Connect");
            dos.writeUTF(value.toString(null));
        } catch (Exception ex) {
            plugin.nag("An error occured on trying to encode bungee server '" + value + "'");
            plugin.nag(ex);
            return null;
        }
        return bytes.toByteArray();
    }

    @Override
    protected boolean executeCached(AnimatedMenuPlugin plugin, Player p, byte[] value) {
        p.sendPluginMessage(plugin, "BungeeCord", value);
        return true;
    }
}
