package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class TextCommand extends Command<StringBundle, String> {

    public static final TextCommand DEFAULT = ofPlayer("", false, Player::performCommand);

    public static TextCommand ofPlayer(String prefix, boolean color, BiConsumer<Player, String> executor) {
        return new TextCommand(prefix, color) {
            @Override
            protected boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
                executor.accept(p, value);
                return true;
            }
        };
    }

    public static TextCommand of(String prefix, boolean color, Consumer<String> executor) {
        return new TextCommand(prefix, color) {
            @Override
            protected boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
                executor.accept(value);
                return true;
            }
        };
    }

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
