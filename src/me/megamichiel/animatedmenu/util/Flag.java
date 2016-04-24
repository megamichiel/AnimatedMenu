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
    
    public boolean booleanValue()
    {
        switch (this)
        {
        case FALSE:
            return false;
        default:
            return true;
        }
    }

    public static boolean parseBoolean(ConfigurationSection section, String key, boolean def)
    {
        return parseBoolean(section.getString(key), def);
    }

    public static boolean parseBoolean(String str, boolean def)
    {
        Flag flag = parseFlag(str, def ? Flag.TRUE : Flag.FALSE);
        return flag != Flag.BOTH && flag.booleanValue();
    }

    public static Flag parseFlag(ConfigurationSection section, String key, Flag def)
    {
        return parseFlag(section.getString(key), def);
    }

    private static final Map<String, Flag> flags = new ImmutableMap.Builder<String, Flag>()
            .put("true", Flag.TRUE).put("yes", Flag.TRUE).put("on", Flag.TRUE).put("enable", Flag.TRUE)
            .put("both", Flag.BOTH).put("all", Flag.BOTH).build();

    public static Flag parseFlag(String str, Flag def)
    {
        if (str == null) return def;
        Flag flag = flags.get(str.toLowerCase());
        return flag == null ? Flag.FALSE : flag;
    }
}