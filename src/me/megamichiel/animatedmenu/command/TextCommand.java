package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.entity.Player;

public abstract class TextCommand extends Command<StringBundle, String> {

    public TextCommand(String prefix) {
        super(prefix);
    }

    @Override
    public StringBundle parse(Nagger nagger, String command) {
        return StringBundle.parse(nagger, command).colorAmpersands();
    }

    @Override
    public String tryCacheValue(AnimatedMenuPlugin plugin, StringBundle value) {
        return value.containsPlaceholders() ? null : value.toString(null);
    }

    @Override
    public boolean execute(AnimatedMenuPlugin plugin, Player p, StringBundle value) {
        return executeCached(plugin, p, value.toString(p));
    }
}
