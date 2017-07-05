package me.megamichiel.animatedmenu.util.item;

import com.google.common.base.Predicate;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bukkit.nbt.NBTModifiers;
import me.megamichiel.animationlib.bukkit.nbt.NBTUtil;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class Skull implements MaterialSpecific.Action<SkullMeta> {
    
    private static final Field skullProfile;
    private static final Method fillProfile, serializeProfile;

    private static final String USERNAME_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    static {
        Field field = null;
        Method method = null, method1 = null;
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            field = Class.forName(pkg + ".inventory.CraftMetaSkull").getDeclaredField("profile");
            field.setAccessible(true);

            pkg = "net.minecraft.server" + pkg.substring(pkg.lastIndexOf('.'));

            Class<?> clazz = Class.forName(pkg + ".TileEntitySkull");
            for (Method m : clazz.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length > 0 && params[0] == GameProfile.class) {
                        method = m;
                        break;
                    }
                }
            }
            method1 = Class.forName(pkg + ".GameProfileSerializer").getDeclaredMethod(
                    "serialize", Class.forName(pkg + ".NBTTagCompound"), field.getType());
        } catch (Exception ex) {
            // Not supported
        }
        skullProfile = field;
        fillProfile = method;
        serializeProfile = method1;
    }

    public static Skull forName(String name) {
        Skull skull = new Skull(null, name);

        if (!cachedProfiles.containsKey(name)) loadProfile(name);

        return skull;
    }
    
    private static final Map<String, GameProfile> cachedProfiles = new ConcurrentHashMap<>();
    
    private final StringBundle name;
    
    public Skull(Nagger nagger, String name) {
        this.name = StringBundle.parse(nagger, name);
    }

    @Override
    public void apply(Player player, SkullMeta meta) {
        String name = this.name.toString(player);
        GameProfile profile = cachedProfiles.get(name);
        if (profile != null) {
            if (profile.getName() != null) {
                try {
                    skullProfile.set(meta, profile);
                } catch (Exception ex) {
                    // No support ;c
                }
            }
        } else {
            loadProfile(name);
            meta.setOwner(name);
        }
    }

    public void apply(Player player, Map<String, Object> map) {
        String name = this.name.toString(player);
        GameProfile profile = cachedProfiles.get(name);
        if (profile != null) {
            if (profile.getName() != null) {
                try {
                    map.put("SkullOwner", serializeProfile.invoke(null, NBTUtil.getInstance().createTag(), profile));
                } catch (Exception ex) {
                    // No support ;c
                }
            }
        } else {
            loadProfile(name);
            map.put("SkullOwner", NBTModifiers.STRING.wrap(name));
        }
    }

    public ItemStack toItemStack(Player player, int amount, Consumer<ItemMeta> meta) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, amount, (short) 3);
        ItemMeta im = item.getItemMeta();
        apply(player, (SkullMeta) im);
        meta.accept(im);
        return item;
    }

    public ItemStack toItemStack(Player player, Consumer<ItemMeta> meta) {
        return toItemStack(player, 1, meta);
    }

    private static void load(final String savedName, String name) {
        final GameProfile profile = new GameProfile(null, name);
        cachedProfiles.put(savedName, profile);
        try {
            if (fillProfile.getParameterTypes().length == 2) { // Spigot
                fillProfile.invoke(null, profile, (Predicate<GameProfile>) profile1 -> {
                    if (profile1 != null)
                        cachedProfiles.put(savedName, profile1);
                    return false;
                });
            } else if (fillProfile.getParameterTypes().length == 1) { // Bukkit
                cachedProfiles.put(savedName, (GameProfile) fillProfile.invoke(null, profile));
            }
        } catch (Exception ex) {
            // No support ;c
        }
    }
    
    public static void loadProfile(final String name) {
        int length = name.length();
        if (length <= 36) { // uuid/name
            final GameProfile profile;
            if (length <= 16) { // name :O
                load(name, name);
                return;
            } else if (length == 32) { // non-hyphen uuid
                profile = new GameProfile(UUID.fromString(name.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), null);
            } else if (length == 36) { // hyphen uuid
                profile = new GameProfile(UUID.fromString(name), null);
            } else { // idk D:
                cachedProfiles.put(name, new GameProfile(UUID.randomUUID(), null));
                return;
            }
            cachedProfiles.put(name, profile);
            // Load skin from UUID asynchronous:
            Thread thread = new Thread(() -> {
                UUID uuid = profile.getId();
                try {
                    URLConnection connection = new URL("https://api.mojang.com/user/profiles/"
                                    + uuid.toString().replace("-", "") + "/names").openConnection();
                    InputStream in = connection.getInputStream();
                    byte[] data = new byte[in.available()];
                    ByteStreams.readFully(in, data);
                    JsonArray array = (JsonArray) new JsonParser().parse(new String(data, "UTF-8"));
                    String currentName = array.get(array.size() - 1).getAsJsonObject().get("name").getAsString();
                    load(name, currentName);
                } catch (IOException ex) {
                    // Cannot connect/no such player
                }
            });
            thread.setDaemon(true);
            thread.start();
        } else { // json?
            String base64;
            try {
                Base64.getDecoder().decode(name);
                base64 = name;
            } catch (IllegalArgumentException ex) {
                try {
                    base64 = "{textures:{SKIN:{url:\"" + URLEncoder.encode(name, "UTF-8") + "\"}}}";
                } catch (UnsupportedEncodingException ex2) {
                    cachedProfiles.put(name, new GameProfile(UUID.randomUUID(), null));
                    return;
                }
            }
            try {
                char[] chars = new char[16];
                Random random = ThreadLocalRandom.current();
                for (int i = 0; i < 16; ++i) {
                    chars[i] = USERNAME_CHARS.charAt(random.nextInt(36));
                }
                GameProfile profile = new GameProfile(UUID.randomUUID(), new String(chars));
                profile.getProperties().put("textures", new Property("textures", base64));
                cachedProfiles.put(name, profile);
            } catch (Exception ex) {
                cachedProfiles.put(name, new GameProfile(UUID.randomUUID(), null));
            }
        }
    }
}
