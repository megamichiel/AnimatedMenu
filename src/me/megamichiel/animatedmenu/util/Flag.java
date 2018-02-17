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

    public static boolean parseBoolean(String str) {
        return parseBoolean(str, false);
    }

    public static boolean parseBoolean(String str, boolean def) {
        return str == null ? def : flags.getOrDefault(str.toLowerCase(Locale.ENGLISH), def ? TRUE : FALSE) == TRUE;
    }

    private static final Map<String, Flag> flags = new ImmutableMap.Builder<String, Flag>()
            .put("true", TRUE).put("false", FALSE)
            .put("yes", TRUE).put("no", FALSE)
            .put("on", TRUE).put("off", FALSE)
            .put("enable", TRUE).put("disable", FALSE)
            .put("both", BOTH).put("all", BOTH)
            .build();

    public static Flag parseFlag(String str, Flag def) {
        return str == null ? def : flags.getOrDefault(str.toLowerCase(Locale.ENGLISH), def);
    }
}
