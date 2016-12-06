package me.megamichiel.animatedmenu.animation;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.menu.MenuType;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.Animatable;

import java.util.Locale;

public class AnimatedOpenAnimation extends Animatable<OpenAnimation> {

    private AnimatedMenuPlugin plugin;
    private final MenuType type;
    private double speed;

    public AnimatedOpenAnimation(MenuType type) {
        this.type = type;
    }

    public void init(AnimatedMenuPlugin plugin, double speed) {
        this.plugin = plugin;
        this.speed = speed;
    }

    @Override
    protected OpenAnimation convert(Nagger nagger, Object o) {
        String s = o.toString();
        try {
            String type;
            double speed;

            int i = s.indexOf(':');
            if (i == -1) {
                type = s;
                speed = this.speed;
            } else {
                type = s.substring(0, i);
                speed = Double.parseDouble(s.substring(i + 1));
                if (speed <= 0) speed = this.speed;
            }
            OpenAnimation.Type animationType = plugin.resolveAnimationType(
                    type.toUpperCase(Locale.ENGLISH).replace('-', '_'));
            if (animationType == null) {
                nagger.nag("Unknown animation type: " + type);
                return null;
            }
            return new OpenAnimation(animationType.sort(this.type), speed);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
