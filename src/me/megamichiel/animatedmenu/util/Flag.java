package me.megamichiel.animatedmenu.util;

import com.google.common.collect.ImmutableMap;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

public enum Flag {
    
    TRUE {
        @Override
        public boolean matches(boolean b) {
            return b;
        }
    }, FALSE {
        @Override
        public boolean matches(boolean b) {
            return !b;
        }
    }, BOTH {
        @Override
        public boolean matches(boolean b) {
            return true;
        }
    };
    
    public abstract boolean matches(boolean b);
    
    public boolean booleanValue() {
        return this != FALSE;
    }

    public static boolean parseBoolean(String str, boolean def) {
        Flag flag = parseFlag(str, def ? Flag.TRUE : Flag.FALSE);
        return flag != Flag.BOTH && flag.booleanValue();
    }

    private static final Map<String, Flag> flags = new ImmutableMap.Builder<String, Flag>()
            .put("true", Flag.TRUE).put("yes", Flag.TRUE).put("on", Flag.TRUE).put("enable", Flag.TRUE)
            .put("both", Flag.BOTH).put("all", Flag.BOTH).build();

    public static Flag parseFlag(String str, Flag def) {
        if (str == null) return def;
        Flag flag = flags.get(str.toLowerCase());
        return flag == null ? Flag.FALSE : flag;
    }
}
