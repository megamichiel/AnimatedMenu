package me.megamichiel.animatedmenu.command;

import java.util.List;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class ClickExclusiveCommandExecutor extends CommandExecutor {
	
	private final boolean rightClick;
	
	public ClickExclusiveCommandExecutor(AnimatedMenuPlugin plugin, List<?> commands, boolean rightClick) {
		super(plugin, commands);
		this.rightClick = rightClick;
	}
	
	@Override
	public void execute(AnimatedMenuPlugin plugin, Player p, ClickType click) {
		if((click.isRightClick() && rightClick) || (click.isLeftClick() && !rightClick))
			super.execute(plugin, p, click);
	}
	
	public static class ClickExclusiveShiftCommandExecutor extends ClickExclusiveCommandExecutor {
		
		private final boolean shiftClick;
		
		public ClickExclusiveShiftCommandExecutor(AnimatedMenuPlugin plugin,
				List<?> commands, boolean rightClick, boolean shiftClick) {
			super(plugin, commands, rightClick);
			this.shiftClick = shiftClick;
		}
		
		@Override
		public void execute(AnimatedMenuPlugin plugin, Player p, ClickType click) {
			if(click.isShiftClick() == shiftClick)
				super.execute(plugin, p, click);
		}
	}
}
