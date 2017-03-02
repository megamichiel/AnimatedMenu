package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.entity.Player;

import java.util.Locale;

public class SoundCommand extends Command<StringBundle, SoundCommand.SoundInfo> {

    public SoundCommand() {
        super("sound");
    }

    @Override
    public StringBundle parse(Nagger nagger, String command) {
        return StringBundle.parse(nagger, command).colorAmpersands();
    }

    @Override
    public boolean execute(AnimatedMenuPlugin plugin, Player p, StringBundle value) {
        new SoundInfo(plugin, value.toString(p)).play(p);
        return true;
    }

    @Override
    public SoundInfo tryCacheValue(AnimatedMenuPlugin plugin, StringBundle value) {
        return value.containsPlaceholders() ? null : new SoundInfo(plugin, value.toString(null));
    }

    @Override
    public boolean executeCached(AnimatedMenuPlugin plugin, Player p, SoundInfo value) {
        value.play(p);
        return true;
    }

    public static class SoundInfo {

        private final Nagger nagger;
        private final String sound;
        private final float volume, pitch;

        public SoundInfo(Nagger nagger, String value) {
            this.nagger = nagger;
            String[] split = value.split(" ");
            sound = split[0].toLowerCase(Locale.ENGLISH).replace('-', '_').replace('.', '_');
            float volume = 0, pitch = 0;
            if (split.length > 1) {
                try {
                    volume = Float.parseFloat(split[1]);
                } catch (NumberFormatException ex) {
                    nagger.nag("Invalid volume: " + split[1]);
                }
                if (split.length > 2) try {
                    pitch = Float.parseFloat(split[2]);
                } catch (NumberFormatException ex) {
                    nagger.nag("Invalid pitch: " + split[2]);
                }
            }
            this.volume = volume;
            this.pitch = pitch;
        }

        public SoundInfo(Nagger nagger, String sound, float volume, float pitch) {
            this.nagger = nagger;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public void play(Player player) {
            try {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException ex) {
                nagger.nag("No sound by name '" + sound + "' found!");
            }
        }
    }
}
