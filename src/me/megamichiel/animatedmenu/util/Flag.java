package me.megamichiel.animatedmenu.util;

import com.google.common.collect.ImmutableMap;

import java.util.Locale;
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
        Flag flag = parseFlag(str, def ? TRUE : FALSE);
        return flag != BOTH && flag.booleanValue();
    }

    private static final Map<String, Flag> flags = new ImmutableMap.Builder<String, Flag>()
            .put("true", TRUE).put("yes", TRUE).put("on", TRUE).put("enable", TRUE)
            .put("both", BOTH).put("all", BOTH).build();

    public static Flag parseFlag(String str, Flag def) {
        if (str == null) return def;
        Flag flag = flags.get(str.toLowerCase(Locale.US));
        return flag == null ? FALSE : flag;
    }
}
