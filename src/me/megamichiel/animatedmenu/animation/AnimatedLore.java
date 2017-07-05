package me.megamichiel.animatedmenu.animation;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.Animatable;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class AnimatedLore extends Animatable<StringBundle[]> {

    private File imagesFolder;

    public Stream<String> stream(Player player) {
        return Arrays.stream(get()).map(sb -> sb.toString(player));
    }

    public boolean load(AnimatedMenuPlugin plugin, AbstractConfig section, String key) {
        imagesFolder = new File(plugin.getDataFolder(), "images");
        return super.load(plugin, section, key);
    }

    @Override
    protected StringBundle[] defaultValue() {
        return new StringBundle[0];
    }

    @Override
    protected Object getValue(Nagger nagger, AbstractConfig section, String key) {
        return section.getStringList(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected StringBundle[] convert(Nagger nagger, Object o) {
        List<StringBundle> frame = new ArrayList<>();
        for (String item : (List<String>) o) {
            if (item.toLowerCase(Locale.ENGLISH).startsWith("file:")) {
                File file = new File(imagesFolder, item.substring(5).trim());
                if (file.isFile()) {
                    try {
                        readImage(file, frame);
                    } catch (IOException e) {
                        nagger.nag("Failed to read file " + file.getName() + "!");
                        nagger.nag(e);
                    }
                    continue;
                }
            }
            frame.add(StringBundle.parse(nagger, item).colorAmpersands());
        }
        return frame.toArray(new StringBundle[0]);
    }

    private static void readImage(File file, List<StringBundle> list) throws IOException {
        BufferedImage img = ImageIO.read(file);
        int height = img.getHeight(), width = img.getWidth();
        for (int y = 0; y < height; y++) {
            StringBuilder sb = new StringBuilder();
            ChatColor previous = null;
            for (int x = 0; x < width; x++) {
                ChatColor color = matchColor(img.getRGB(x, y));
                if (color != previous) {
                    sb.append(color.toString());
                    previous = color;
                }
                sb.append(StringBundle.BOX);
            }
            list.add(new StringBundle(null, sb.toString()));
        }
    }

    private static final int[] colors = {
            0x000000,    // BLACK
            0x0000AA,    // DARK_BLUE
            0x00AA00,    // DARK_GREEN
            0x00AAAA,    // DARK_AQUA
            0xAA0000,    // DARK_RED
            0xAA00AA,    // DARK_PURPLE
            0xFFAA00,    // GOLD
            0xAAAAAA,    // GRAY
            0x555555,    // DARK_GRAY
            0x5555FF,    // BLUE
            0x55FF55,    // GREEN
            0x55FFFF,    // AQUA
            0xFF5555,    // RED
            0xFF55FF,    // LIGHT_PURPLE
            0xFFFF55,    // YELLOW
            0xFFFFFF     // WHITE
    };

    private static ChatColor matchColor(int rgb) {
        int index = 0;
        for (int i = colors.length; --i >= 0; ) {
            if (Math.abs(colors[i] - rgb) < Math.abs(colors[index] - rgb)) {
                index = i;
            }
        }
        return ChatColor.values()[index];
    }
}
