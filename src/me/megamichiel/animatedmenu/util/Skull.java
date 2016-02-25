package me.megamichiel.animatedmenu.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;

import com.google.common.base.Predicate;
import com.mojang.authlib.GameProfile;

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
	
	private static final Map<String, GameProfile> cachedProfiles = new ConcurrentHashMap<String, GameProfile>();
	
	private final StringBundle name;
	
	public Skull(Nagger nagger, String name) {
		this.name = StringUtil.parseBundle(nagger, name);
	}
	
	public void apply(Player player, SkullMeta meta)
	{
		String name = this.name.toString(player);
		GameProfile profile = cachedProfiles.get(name);
		if (profile != null)
		{
			try
			{
				skullProfile.set(meta, profile);
			}
			catch (Exception ex) {}
		}
		else
		{
			load(name);
			meta.setOwner(name);
		}
	}
	
	private void load(String name)
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
}
