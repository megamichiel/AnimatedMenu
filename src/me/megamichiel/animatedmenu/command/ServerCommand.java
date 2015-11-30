package me.megamichiel.animatedmenu.command;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import org.bukkit.entity.Player;

public class ServerCommand extends Command {
	
	public ServerCommand(String command) {
		super(command);
	}
	
	@Override
	public void execute(Player p) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeUTF("Connect");
			dos.writeUTF(command);
		} catch (Exception ex) {
			AnimatedMenuPlugin.getInstance().getLogger().warning("An error occured on trying to connect a player to '" + command + "'");
			return;
		}
		p.sendPluginMessage(AnimatedMenuPlugin.getInstance(), "BungeeCord", out.toByteArray());
	}
}
