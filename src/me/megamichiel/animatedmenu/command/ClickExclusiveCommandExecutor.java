package me.megamichiel.animatedmenu.command;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class ClickExclusiveCommandExecutor extends CommandExecutor {
	
	private final boolean rightClick;
	
	public ClickExclusiveCommandExecutor(List<String> commands, boolean rightClick) {
		super(commands);
		this.rightClick = rightClick;
	}
	
	@Override
	public void execute(Player p, ClickType click) {
		if((click.isRightClick() && rightClick) || (click.isLeftClick() && !rightClick))
			super.execute(p, click);
	}
}
