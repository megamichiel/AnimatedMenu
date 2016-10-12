package me.megamichiel.animatedmenu.animation;

import me.megamichiel.animatedmenu.animation.AnimatedLore.Frame;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.Animatable;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class AnimatedLore extends Animatable<Frame> {
    
    private static final long serialVersionUID = 6241886968360414688L;

    private final Plugin plugin;

    public AnimatedLore(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    protected Frame defaultValue() {
        return new Frame();
    }

    private Frame loadFrame(Nagger nagger, List<String> list) {
        Frame frame = new Frame();
        for (String item : list) {
            if (item.toLowerCase(Locale.US).startsWith("file: ")) {
                File file = new File(plugin.getDataFolder(), "images/" + item.substring(6));
                if (file.exists() && file.isFile()) {
                    try {
                        frame.addAll(readImage(file));
                    } catch (IOException e) {
                        nagger.nag("Failed to read file " + file.getName() + "!");
                        nagger.nag(e);
                    }
                    continue;
                }
            }
            frame.add(StringBundle.parse(nagger, item).colorAmpersands().tryCache());
        }
        return frame;
    }

    @Override
    protected Object getValue(Nagger nagger, AbstractConfig section, String key) {
        return section.getStringList(key);
    }

    @Override
    protected Frame convert(Nagger nagger, Object o) {
        return loadFrame(nagger, (List<String>) o);
    }

    @Override
    public AnimatedLore clone() {
        AnimatedLore lore = (AnimatedLore) super.clone();
        for (int i = lore.size(); i-- != 0;)
            lore.set(i, new Frame(lore.get(i)));
        return lore;
    }

    public class Frame extends ArrayList<IPlaceholder<String>> {
        
        private static final long serialVersionUID = 3655877616632972680L;
        
        public Frame() {
            super();
        }
        
        public Frame(Collection<? extends IPlaceholder<String>> list) {
            super(list);
        }

        public Frame(StringBundle... bundles) {
            super(Arrays.asList(bundles));
        }
        
        public List<String> toStringList(Nagger nagger, Player p) {
            List<String> list = new ArrayList<>(size());
            this.stream().map(b -> b.invoke(nagger, p)).forEach(list::add);
            return list;
        }
        
        public String toString(Nagger nagger, Player p) {
            StringBuilder sb = new StringBuilder();
            for (IPlaceholder<String> bundle : this)
                sb.append(bundle.invoke(nagger, p));
            return sb.toString();
        }
    }

    private static List<IPlaceholder<String>> readImage(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        int height = img.getHeight(), width = img.getWidth();
        List<IPlaceholder<String>> lines = new ArrayList<>(height);
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
            lines.set(y, IPlaceholder.constant(sb.toString()));
        }
        return lines;
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
        for (int i = 0; i < colors.length; i++)
            if (Math.abs(colors[i] - rgb) < Math.abs(colors[index] - rgb))
                index = i;
        return ChatColor.values()[index];
    }
}
