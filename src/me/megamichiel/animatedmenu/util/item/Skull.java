package me.megamichiel.animatedmenu.util.item;

import com.google.common.base.Predicate;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bukkit.nbt.NBTUtil;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Skull implements MaterialSpecific.Action<SkullMeta> {

    private static final char[] USERNAME_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_".toCharArray();

    private static final Map<String, GameProfile> cachedProfiles = new ConcurrentHashMap<>();

    private static final Field skullProfile;
    private static final Method getProfile, fillProfile, serializeProfile, deserializeProfile;

    static {
        Field _skullProfile = null;
        Method _getProfile = null, _fillProfile = null, _serializeProfile = null, _deserializeProfile = null;
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            _skullProfile = Class.forName(pkg + ".inventory.CraftMetaSkull").getDeclaredField("profile");
            _skullProfile.setAccessible(true);

            _getProfile = Class.forName(pkg + ".entity.CraftPlayer").getDeclaredMethod("getProfile");

            pkg = "net.minecraft.server" + pkg.substring(pkg.lastIndexOf('.'));

            Class<?> clazz = Class.forName(pkg + ".TileEntitySkull"),
                  compound = Class.forName(pkg + ".NBTTagCompound");
            Class<?>[] params;
            for (Method m : clazz.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && (params = m.getParameterTypes()).length > 0 && params[0] == GameProfile.class) {
                    _fillProfile = m;
                    break;
                }
            }

            _serializeProfile = Class.forName(pkg + ".GameProfileSerializer").getDeclaredMethod(
                    "serialize", compound, _skullProfile.getType()
            );
            _deserializeProfile = Class.forName(pkg + ".GameProfileSerializer").getDeclaredMethod(
                    "deserialize", compound
            );
        } catch (Exception ex) {
            // Not supported
        }
        skullProfile = _skullProfile;
        fillProfile = _fillProfile;
        serializeProfile = _serializeProfile;
        deserializeProfile = _deserializeProfile;
        getProfile = _getProfile;
    }

    public static Skull forName(String name) {
        Skull skull = new Skull(null, name);

        if (!cachedProfiles.containsKey(name)) {
            loadProfile(name, name);
        }

        return skull;
    }

    private final StringBundle name;
    
    public Skull(Nagger nagger, Object name) {
        this.name = StringBundle.parse(nagger, name.toString());
    }

    public Stream<?> stream() {
        return name.stream();
    }

    @Override
    public void apply(Player player, SkullMeta meta, PlaceholderContext context) {
        String name = this.name.toString(player, context);
        GameProfile profile = cachedProfiles.get(name);
        if ((profile == null ? (profile = loadProfile(name, name)) : profile).getName() != null) {
            try {
                skullProfile.set(meta, profile);
            } catch (Exception ex) {
                ex.printStackTrace();
                // No support ;c
            }
        }
    }

    public void apply(Player player, Map<String, Object> map, PlaceholderContext context) {
        String name = this.name.toString(player, context);
        GameProfile profile = cachedProfiles.get(name);
        if ((profile == null ? (profile = loadProfile(name, name)) : profile).getName() != null) {
            try {
                map.put("SkullOwner", serializeProfile.invoke(null, NBTUtil.getInstance().createTag(), profile));
            } catch (Exception ex) {
                // No support ;c
            }
        }
    }

    public ItemStack toItemStack(Player player, int amount, Consumer<ItemMeta> meta, PlaceholderContext context) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, amount, (short) 3);
        ItemMeta im = item.getItemMeta();
        apply(player, (SkullMeta) im, context);
        meta.accept(im);
        return item;
    }

    public ItemStack toItemStack(Player player, Consumer<ItemMeta> meta, PlaceholderContext context) {
        return toItemStack(player, 1, meta, context);
    }
    
    private static GameProfile loadProfile(String savedName, String name) {
        GameProfile profile = null;
        if (name.startsWith("hdb:")) {
            String id = name.substring(4).trim();
            try {
                ItemStack item = new HeadDatabaseAPI().getItemHead(id);
                if (item != null) {
                    ItemMeta meta = item.getItemMeta();
                    try {
                        profile = (GameProfile) skullProfile.get(meta);
                    } catch (Exception ex) {
                        // Failed ;c
                    }
                    if (profile == null) { // Profile access failed or field is actually null. Possibly stored in unhandledTags field
                        NBTUtil util = NBTUtil.getInstance();
                        Object tag = util.createTag();
                        util.getMap(tag).putAll(util.getMap(meta));
                        try {
                            profile = (GameProfile) deserializeProfile.invoke(null, tag);
                        } catch (Exception ex) {
                            // Failed again ;c
                        }
                    }
                }
            } catch (NoClassDefFoundError err) {
                // No head database ;c
            }
            if (profile == null) {
                profile = new GameProfile(UUID.randomUUID(), "Dummy");
            } else {
                GameProfile copy = new GameProfile(profile.getId(), "Dummy");
                copy.getProperties().putAll(profile.getProperties());
                profile = copy;
            }
            cachedProfiles.put(savedName, profile);
            return profile;
        }
        int length = name.length();

        switch (length) {
            case 32:
                try {
                    profile = new GameProfile(UUID.fromString(
                            name.substring(0, 8) + '-' +
                            name.substring(8, 12) + '-' +
                            name.substring(12, 16) + '-' +
                            name.substring(16, 20) + '-' +
                            name.substring(20, 32)
                    ), null);
                } catch (IllegalArgumentException ex) {
                    // Not a UUID
                }
                break;
            case 36:
                try {
                    profile = new GameProfile(UUID.fromString(name), null);
                } catch (IllegalArgumentException ex) {
                    // Not a UUID
                }
        }
        if (profile != null) {
            UUID id = profile.getId();
            if (Bukkit.getOnlineMode()) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) { // Player is online? Use their skin!
                    try {
                        cachedProfiles.put(savedName, profile = (GameProfile) getProfile.invoke(player));
                        return profile;
                    } catch (Exception ex) {
                        // At least I tried ;c
                    }
                }
            }

            cachedProfiles.put(savedName, profile);
            // Load skin from UUID asynchronous:
            Thread thread = new Thread(() -> {
                try {
                    URLConnection connection = new URL("https://api.mojang.com/user/profiles/" + id.toString().replace("-", "") + "/names").openConnection();
                    InputStream in = connection.getInputStream();
                    byte[] data = new byte[in.available()];
                    ByteStreams.readFully(in, data);
                    JsonArray array = (JsonArray) new JsonParser().parse(new String(data, "UTF-8"));
                    loadProfile(savedName, array.get(array.size() - 1).getAsJsonObject().get("name").getAsString());
                } catch (IOException | IndexOutOfBoundsException ex) {
                    // Cannot connect/no such player
                }
            });
            thread.setDaemon(true);
            thread.start();
            return profile;
        }
        if (length <= 16) {
            if (Bukkit.getOnlineMode()) {
                Player player = Bukkit.getPlayerExact(name);
                if (player != null) { // Player is online? Use their skin!
                    try {
                        cachedProfiles.put(savedName, profile = (GameProfile) getProfile.invoke(player));
                        return profile;
                    } catch (Exception ex) {
                        // At least I tried ;c
                    }
                }
            }

            cachedProfiles.put(savedName, profile = new GameProfile(UUID.randomUUID(), name));
            try {
                switch (fillProfile.getParameterTypes().length) {
                    case 1: // CraftBukkit
                        cachedProfiles.put(savedName, profile = (GameProfile) fillProfile.invoke(null, profile));
                        break;
                    case 2: // Spigot
                        fillProfile.invoke(null, profile, (Predicate<GameProfile>) $profile -> {
                            if ($profile != null) {
                                cachedProfiles.put(savedName, $profile);
                            }
                            return false;
                        });
                }
            } catch (Exception ex) {
                // No support ;c
            }

            return profile;
        }
        // json?
        String base64;
        try {
            Base64.getDecoder().decode(name);
            base64 = name;
        } catch (IllegalArgumentException ex) {
            base64 = Base64.getEncoder().encodeToString(("{textures:{SKIN:{url:\"" + name + "\"}}}").getBytes(StandardCharsets.UTF_8));
        }
        char[] chars = USERNAME_CHARS, username = new char[16];
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < 16; ++i) {
            username[i] = chars[random.nextInt(chars.length)];
        }
        (profile = new GameProfile(UUID.randomUUID(), new String(username))).getProperties().put("textures", new Property("textures", base64));
        cachedProfiles.put(savedName, profile);

        return profile;
    }
}
