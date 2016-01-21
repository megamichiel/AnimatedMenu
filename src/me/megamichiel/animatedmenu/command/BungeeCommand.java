package me.megamichiel.animatedmenu.command;

import java.io.DataOutput;

import org.bukkit.entity.Player;

import com.google.common.io.ByteStreams;

public class BungeeCommand extends Command {
	
	private final boolean player;
	
	public BungeeCommand(String command, boolean player) {
		super(command);
		this.player = player;
	}
	
	@Override
	public void execute(Player p) {
		try
		{
			DataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Forward");
			out.writeUTF("ALL");
			out.writeUTF("AMCmd");
			out.writeBoolean(player);
			out.writeUTF(command.replace("%p", p.getName()));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
