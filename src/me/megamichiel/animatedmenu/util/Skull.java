package me.megamichiel.animatedmenu.util;

import com.google.common.base.Predicate;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Skull {
    
    private static final Field skullProfile;
    private static final Method fillProfile;
    
    static
    {
        Field field = null;
        Method method = null;
        try
        {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            field = Class.forName(pkg + ".inventory.CraftMetaSkull").getDeclaredField("profile");
            field.setAccessible(true);
            
            Class<?> clazz = Class.forName("net.minecraft.server." + pkg.split("\\.")[3] + ".TileEntitySkull");
            for (Method m : clazz.getDeclaredMethods())
            {
                if (Modifier.isStatic(m.getModifiers()))
                {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length > 0 && params[0] == GameProfile.class)
                    {
                        method = m;
                        break;
                    }
                }
            }
        }
        catch (Exception ex) {}
        skullProfile = field;
        fillProfile = method;
    }
    
    private static final Map<String, GameProfile> cachedProfiles = new ConcurrentHashMap<>();
    
    private final StringBundle name;
    
    public Skull(Nagger nagger, String name) {
        this.name = StringBundle.parse(nagger, name);
    }
    
    public void apply(Player player, SkullMeta meta)
    {
        String name = this.name.toString(player);
        GameProfile profile = cachedProfiles.get(name);
        if (profile != null)
        {
            if (profile.getName() != null) {
                try
                {
                    skullProfile.set(meta, profile);
                }
                catch (Exception ex) {}
            }
        }
        else
        {
            loadProfile(name);
            meta.setOwner(name);
        }
    }
    
    private static void load(String name)
    {
        final GameProfile profile = new GameProfile(null, name);
        cachedProfiles.put(name, profile);
        try
        {
            if (fillProfile.getParameterTypes().length == 2) // Spigot
            {
                fillProfile.invoke(null, profile, new Predicate<GameProfile>() {
                    @Override
                    public boolean apply(GameProfile profile) {
                        if (profile != null)
                            cachedProfiles.put(profile.getName(), profile);
                        return false;
                    }
                });
            }
            else if (fillProfile.getParameterTypes().length == 1) // Bukkit
            {
                cachedProfiles.put(name, (GameProfile) fillProfile.invoke(null, profile));
            }
        }
        catch (Exception ex) {}
    }
    
    public static void loadProfile(String name)
    {
		int length = name.length();
		if (length <= 36) {
			GameProfile profile;
			if (length <= 16) {
                load(name);
                return;
			} else if (length == 32) {
				profile = new GameProfile(UUID.fromString(name.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), null);
			} else if (length == 36) {
				profile = new GameProfile(UUID.fromString(name), null);
			} else {
                cachedProfiles.put(name, new GameProfile(null, name));
                return;
			}
            cachedProfiles.put(name, profile);
		} else {
			try {
                JsonObject obj = new JsonParser().parse(name).getAsJsonObject();
                UUID uuid = UUID.fromString(obj.get("Id").getAsString());
                GameProfile profile = new GameProfile(uuid, name);
                JsonObject textures = obj.getAsJsonObject("Properties");
                profile.getProperties().put("textures",
                        new Property("textures", textures.getAsJsonArray("textures")
                                .get(0).getAsJsonObject().get("Value").getAsString()));
                cachedProfiles.put(name, profile);
			} catch (Exception ex) {
                cachedProfiles.put(name, new GameProfile(null, name));
			}
		}
    }
}
