package me.megamichiel.animatedmenu.util.item;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bukkit.nbt.NBTModifiers;
import me.megamichiel.animationlib.bukkit.nbt.NBTUtil;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@SuppressWarnings("deprecation")
public class BannerPattern implements MaterialSpecific.Action<BannerMeta> {

    private static final DyeColor[] colors = new DyeColor[16];
    
    static {
        for (int i = 0; i < 16; i++) {
            colors[i] = DyeColor.getByDyeData((byte) (15 - i));
        }
    }

    private final Pattern baseColor;
    private final List<Pattern> patterns = new ArrayList<>();
    
    public BannerPattern(Nagger nagger, String pattern) throws IllegalArgumentException {
        List<Pattern> patterns = new ArrayList<>();
        char[] array = pattern.toCharArray();
        if (array.length % 2 != 0) throw new IllegalArgumentException("Banner pattern length must be a multiple of 2!");
        for (int length = array.length, index = 0; index < length; index += 2) {
            char color = array[index], type = array[index + 1];
            if (color < 'a' || color > 'p') {
                nagger.nag("No pattern color found by identifier '" + color + "'!");
                continue;
            }
            BannerPatternType patternType = BannerPatternType.getByIdentifier(type);
            if (patternType == null) {
                nagger.nag("No pattern type found by identifier '" + type + "'!");
                continue;
            }
            patterns.add(new Pattern(colors[color - 'a'], patternType.patternType));
        }
        Iterator<Pattern> it = patterns.iterator();
        if (it.hasNext()) {
            baseColor = it.next();
            it.forEachRemaining(this.patterns::add);
        } else baseColor = null;
    }

    @Override
    public void apply(Player player, BannerMeta meta, PlaceholderContext context) {
        if (baseColor != null) {
            meta.setBaseColor(baseColor.getColor());
            meta.setPatterns(patterns);
        }
    }

    public void apply(NBTUtil util, Map<String, Object> map) {
        if (baseColor != null) {
            Object entityTag = util.createTag();
            Map<String, Object> entityMap = util.getMap(entityTag);

            entityMap.put("Base", NBTModifiers.INT.wrap((int) baseColor.getColor().getDyeData()));
            Object patterns = util.createList((byte) 10);
            List<Object> list = util.getList(patterns);
            for (Pattern p : this.patterns) {
                Object entry = util.createTag();
                Map<String, Object> entryMap = util.getMap(entry);
                entryMap.put("Color", NBTModifiers.INT.wrap((int) p.getColor().getDyeData()));
                entryMap.put("Pattern", NBTModifiers.STRING.wrap(p.getPattern().getIdentifier()));
                list.add(entry);
            }
            entityMap.put("Patterns", patterns);
            map.put("BlockEntityTag", entityTag);
        }
    }

    public enum BannerPatternType {
        
        BASE(PatternType.BASE, 'a'),
        SQUARE_BOTTOM_LEFT(PatternType.SQUARE_BOTTOM_LEFT, 'b'),
        SQUARE_BOTTOM_RIGHT(PatternType.SQUARE_BOTTOM_RIGHT, 'd'),
        SQUARE_TOP_LEFT(PatternType.SQUARE_TOP_LEFT, 'C'),
        SQUARE_TOP_RIGHT(PatternType.SQUARE_TOP_RIGHT, 'D'),
        STRIPE_BOTTOM(PatternType.STRIPE_BOTTOM, 'f'),
        STRIPE_TOP(PatternType.STRIPE_TOP, 'E'),
        STRIPE_LEFT(PatternType.STRIPE_LEFT, 's'),
        STRIPE_RIGHT(PatternType.STRIPE_RIGHT, 'y'),
        STRIPE_CENTER(PatternType.STRIPE_CENTER, 'l'),
        STRIPE_MIDDLE(PatternType.STRIPE_MIDDLE, 'w'),
        STRIPE_DOWNRIGHT(PatternType.STRIPE_DOWNRIGHT, 'n'),
        STRIPE_DOWNLEFT(PatternType.STRIPE_DOWNLEFT, 'm'),
        STRIPE_SMALL(PatternType.STRIPE_SMALL, 'B'),
        CROSS(PatternType.CROSS, 'j'),
        STRAIGHT_CROSS(PatternType.STRAIGHT_CROSS, 'z'),
        TRIANGLE_BOTTOM(PatternType.TRIANGLE_BOTTOM, 'g'),
        TRIANGLE_TOP(PatternType.TRIANGLE_TOP, 'F'),
        TRIANGLES_BOTTOM(PatternType.TRIANGLES_BOTTOM, 'h'),
        TRIANGLES_TOP(PatternType.TRIANGLES_TOP, 'G'),
        DIAGONAL_LEFT(PatternType.DIAGONAL_LEFT, 'r'),
        DIAGONAL_RIGHT(PatternType.DIAGONAL_RIGHT, 'J'),
        DIAGONAL_LEFT_MIRROR(PatternType.DIAGONAL_LEFT_MIRROR, 'l'),
        DIAGONAL_RIGHT_MIRROR(PatternType.DIAGONAL_RIGHT_MIRROR, 'x'),
        CIRCLE_MIDDLE(PatternType.CIRCLE_MIDDLE, 't'),
        RHOMBUS_MIDDLE(PatternType.RHOMBUS_MIDDLE, 'v'),
        HALF_VERTICAL(PatternType.HALF_VERTICAL, 'H'),
        HALF_HORIZONTAL(PatternType.HALF_HORIZONTAL, 'q'),
        HALF_VERTICAL_MIRROR(PatternType.HALF_VERTICAL_MIRROR, 'M'),
        HALF_HORIZONTAL_MIRROR(PatternType.HALF_HORIZONTAL_MIRROR, 'L'),
        BORDER(PatternType.BORDER, 'c'),
        CURLY_BORDER(PatternType.CURLY_BORDER, 'i'),
        CREEPER(PatternType.CREEPER, 'k'),
        GRADIENT(PatternType.GRADIENT, 'p'),
        GRADIENT_UP(PatternType.GRADIENT_UP, 'K'),
        BRICKS(PatternType.BRICKS, 'e'),
        SKULL(PatternType.SKULL, 'A'),
        FLOWER(PatternType.FLOWER, 'o'),
        MOJANG(PatternType.MOJANG, 'u');
        
        private final PatternType patternType;
        private final char identifier;

        BannerPatternType(PatternType patternType, char identifier) {
            this.patternType = patternType;
            this.identifier = identifier;
        }

        public static BannerPatternType getByIdentifier(char identifier) {
            for (BannerPatternType patternType : values())
                if (patternType.identifier == identifier)
                    return patternType;
            return null;
        }
    }
}
