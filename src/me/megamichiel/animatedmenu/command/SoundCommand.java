package me.megamichiel.animatedmenu.command;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

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

        private final Sound sound;
        private final String soundName;
        private final float volume, pitch;

        public SoundInfo(Nagger nagger, String value) {
            String[] split = value.split(" ");
            String str = split[0];

            int length = str.length();
            char[] bukkitName = new char[length], nmsName = new char[length];
            for (int i = 0; i < length; ++i) {
                switch (str.charAt(i)) {
                    case '-':case '_':case '.':
                        bukkitName[i] = '_';
                        nmsName[i] = '.';
                        break;
                    default:
                        bukkitName[i] = Character.toUpperCase(str.charAt(i));
                        nmsName[i] = Character.toLowerCase(str.charAt(i));
                        break;
                }
            }
            Sound sound;
            try {
                sound = Sound.valueOf(new String(bukkitName, 0, length));
            } catch (IllegalArgumentException ex) {
                sound = null;
            }
            this.soundName = (this.sound = sound) == null ? new String(nmsName, 0, length) : null;

            float volume = 1, pitch = 1;
            switch (split.length) {
                default:
                    try {
                        pitch = Float.parseFloat(split[2]);
                    } catch (NumberFormatException ex) {
                        nagger.nag("Invalid pitch: " + split[2]);
                    }
                case 2:
                    try {
                        volume = Float.parseFloat(split[1]);
                    } catch (NumberFormatException ex) {
                        nagger.nag("Invalid volume: " + split[1]);
                    }
                case 1:
            }
            this.volume = volume;
            this.pitch = pitch;
        }

        public void play(Player player) {
            if (sound != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } else {
                player.playSound(player.getLocation(), soundName, volume, pitch);
            }
        }
    }
}
