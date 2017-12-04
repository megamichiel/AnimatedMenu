package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class ServerCommand extends Command<StringBundle, byte[]> {

    private static final byte[] CONNECT_UTF = { 0, 7, 'C', 'o', 'n', 'n', 'e', 'c', 't' };

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
        try {
            bytes.write(CONNECT_UTF, 0, 9);
            new DataOutputStream(bytes).writeUTF(value.toString(p));

            p.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (Exception ex) {
            plugin.nag("An error occured on trying to encode bungee server '" + value + "'");
            plugin.nag(ex);
        }
        return true;
    }

    @Override
    protected byte[] tryCacheValue(AnimatedMenuPlugin plugin, StringBundle value) {
        if (!value.containsPlaceholders()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try {
                bytes.write(CONNECT_UTF, 0, 9);
                new DataOutputStream(bytes).writeUTF(value.toString(null));

                return bytes.toByteArray();
            } catch (Exception ex) {
                plugin.nag("An error occured on trying to encode bungee server '" + value + "'");
                plugin.nag(ex);
            }
        }
        return null;
    }

    @Override
    protected boolean executeCached(AnimatedMenuPlugin plugin, Player p, byte[] value) {
        p.sendPluginMessage(plugin, "BungeeCord", value);
        return true;
    }
}
