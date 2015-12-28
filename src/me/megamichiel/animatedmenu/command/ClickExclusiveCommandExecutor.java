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
	
	public static class ClickExclusiveShiftCommandExecutor extends ClickExclusiveCommandExecutor {
		
		private final boolean shiftClick;
		
		public ClickExclusiveShiftCommandExecutor(List<String> commands, boolean rightClick, boolean shiftClick) {
			super(commands, rightClick);
			this.shiftClick = shiftClick;
		}
		
		@Override
		public void execute(Player p, ClickType click) {
			if(click.isShiftClick() == shiftClick)
				super.execute(p, click);
		}
	}
}
