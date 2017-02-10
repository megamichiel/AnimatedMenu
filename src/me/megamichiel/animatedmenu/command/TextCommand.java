package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.entity.Player;

public abstract class TextCommand extends Command<StringBundle, String> {

    private final boolean color;

    protected TextCommand(String prefix, boolean color) {
        super(prefix);
        this.color = color;
    }

    @Override
    public StringBundle parse(Nagger nagger, String command) {
        StringBundle sb = StringBundle.parse(nagger, command);
        return color ? sb.colorAmpersands() : sb;
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
