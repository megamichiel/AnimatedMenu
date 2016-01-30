package me.megamichiel.animatedmenu.command;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.Nagger;

import org.bukkit.entity.Player;

public class ServerCommand extends Command {
	
	public ServerCommand(Nagger nagger, String command) {
		super(nagger, command);
	}
	
	@Override
	public boolean execute(AnimatedMenuPlugin plugin, Player p) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeUTF("Connect");
			dos.writeUTF(command.toString(p));
		} catch (Exception ex) {
			plugin.nag("An error occured on trying to connect a player to '" + command + "'");
			plugin.nag(ex);
			return true;
		}
		p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
		return true;
	}
}
