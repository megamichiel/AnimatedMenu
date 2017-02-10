package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import org.bukkit.entity.Player;

public class DefaultCommand extends TextCommand {

    DefaultCommand() {
        super(null, false);
    }

    @Override
    public boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
        p.performCommand(value);
        return true;
    }
}
