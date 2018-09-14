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

import static me.megamichiel.animationlib.bukkit.AnimLibPlugin.IS_LEGACY;


//@SuppressWarnings("deprecation")
public class BannerPattern implements MaterialSpecific.Action<BannerMeta> {

    private static final DyeColor[] DYE_COLORS = new DyeColor[16];

    private static final PatternType[] PATTERN_TYPES = new PatternType['z' - 'A' + 1];
    
    static {
        for (int i = 0; i < 16; i++) {
            DYE_COLORS[i] = DyeColor.values()[(byte) (15 - i)];
        }

        PATTERN_TYPES['a' - 'A'] = PatternType.BASE;
        PATTERN_TYPES['b' - 'A'] = PatternType.SQUARE_BOTTOM_LEFT;
        PATTERN_TYPES['d' - 'A'] = PatternType.SQUARE_BOTTOM_RIGHT;
        PATTERN_TYPES['C' - 'A'] = PatternType.SQUARE_TOP_LEFT;
        PATTERN_TYPES['D' - 'A'] = PatternType.SQUARE_TOP_RIGHT;
        PATTERN_TYPES['f' - 'A'] = PatternType.STRIPE_BOTTOM;
        PATTERN_TYPES['E' - 'A'] = PatternType.STRIPE_TOP;
        PATTERN_TYPES['s' - 'A'] = PatternType.STRIPE_LEFT;
        PATTERN_TYPES['y' - 'A'] = PatternType.STRIPE_RIGHT;
        PATTERN_TYPES['l' - 'A'] = PatternType.STRIPE_CENTER;
        PATTERN_TYPES['w' - 'A'] = PatternType.STRIPE_MIDDLE;
        PATTERN_TYPES['n' - 'A'] = PatternType.STRIPE_DOWNRIGHT;
        PATTERN_TYPES['m' - 'A'] = PatternType.STRIPE_DOWNLEFT;
        PATTERN_TYPES['B' - 'A'] = PatternType.STRIPE_SMALL;
        PATTERN_TYPES['j' - 'A'] = PatternType.CROSS;
        PATTERN_TYPES['z' - 'A'] = PatternType.STRAIGHT_CROSS;
        PATTERN_TYPES['g' - 'A'] = PatternType.TRIANGLE_BOTTOM;
        PATTERN_TYPES['F' - 'A'] = PatternType.TRIANGLE_TOP;
        PATTERN_TYPES['h' - 'A'] = PatternType.TRIANGLES_BOTTOM;
        PATTERN_TYPES['G' - 'A'] = PatternType.TRIANGLES_TOP;
        PATTERN_TYPES['r' - 'A'] = PatternType.DIAGONAL_LEFT;
        PATTERN_TYPES['J' - 'A'] = PatternType.DIAGONAL_RIGHT;
        PATTERN_TYPES['l' - 'A'] = PatternType.DIAGONAL_LEFT_MIRROR;
        PATTERN_TYPES['x' - 'A'] = PatternType.DIAGONAL_RIGHT_MIRROR;
        PATTERN_TYPES['t' - 'A'] = PatternType.CIRCLE_MIDDLE;
        PATTERN_TYPES['v' - 'A'] = PatternType.RHOMBUS_MIDDLE;
        PATTERN_TYPES['H' - 'A'] = PatternType.HALF_VERTICAL;
        PATTERN_TYPES['q' - 'A'] = PatternType.HALF_HORIZONTAL;
        PATTERN_TYPES['M' - 'A'] = PatternType.HALF_VERTICAL_MIRROR;
        PATTERN_TYPES['L' - 'A'] = PatternType.HALF_HORIZONTAL_MIRROR;
        PATTERN_TYPES['c' - 'A'] = PatternType.BORDER;
        PATTERN_TYPES['i' - 'A'] = PatternType.CURLY_BORDER;
        PATTERN_TYPES['k' - 'A'] = PatternType.CREEPER;
        PATTERN_TYPES['p' - 'A'] = PatternType.GRADIENT;
        PATTERN_TYPES['K' - 'A'] = PatternType.GRADIENT_UP;
        PATTERN_TYPES['e' - 'A'] = PatternType.BRICKS;
        PATTERN_TYPES['A' - 'A'] = PatternType.SKULL;
        PATTERN_TYPES['o' - 'A'] = PatternType.FLOWER;
        PATTERN_TYPES['u' - 'A'] = PatternType.MOJANG;
    }

    private final Pattern baseColor;
    private final List<Pattern> patterns = new ArrayList<>();
    
    public BannerPattern(Nagger nagger, String pattern) throws IllegalArgumentException {
        List<Pattern> patterns = new ArrayList<>();
        char[] array = pattern.toCharArray();
        if (array.length % 2 != 0) {
            throw new IllegalArgumentException("Banner pattern length must be a multiple of 2!");
        }
        for (int length = array.length, index = 0; index < length; index += 2) {
            char color = array[index], type = array[index + 1];
            if (color < 'a' || color > 'p') {
                nagger.nag("No pattern color found by identifier '" + color + "'!");
                continue;
            }
            PatternType patternType;
            if (type < 'A' || type > 'z' || (patternType = PATTERN_TYPES[type - 'A']) == null) {
                nagger.nag("No pattern type found by identifier '" + type + "'!");
                continue;
            }
            patterns.add(new Pattern(DYE_COLORS[color - 'a'], patternType));
        }
        Iterator<Pattern> it = patterns.iterator();
        if (it.hasNext()) {
            baseColor = it.next();
            it.forEachRemaining(this.patterns::add);
        } else {
            baseColor = null;
        }
    }

    @Override
    public void apply(Player player, BannerMeta meta, PlaceholderContext context) {
        if (baseColor != null) {
            if (IS_LEGACY) {
                meta.setBaseColor(baseColor.getColor());
            }
            meta.setPatterns(patterns);
        }
    }

    public void apply(NBTUtil util, Map<String, Object> map) {
        if (baseColor != null) {
            Object entityTag = util.createTag();
            Map<String, Object> entityMap = util.getMap(entityTag);

            if (IS_LEGACY) {
                entityMap.put("Base", NBTModifiers.INT.wrap(15 - baseColor.getColor().ordinal()));
            }
            Object patterns = util.createList((byte) 10);
            List<Object> list = util.getList(patterns);
            for (Pattern p : this.patterns) {
                Object entry = util.createTag();
                Map<String, Object> entryMap = util.getMap(entry);
                entryMap.put("Color", NBTModifiers.INT.wrap(15 - p.getColor().ordinal()));
                entryMap.put("Pattern", NBTModifiers.STRING.wrap(p.getPattern().getIdentifier()));
                list.add(entry);
            }
            entityMap.put("Patterns", patterns);
            map.put("BlockEntityTag", entityTag);
        }
    }
}
