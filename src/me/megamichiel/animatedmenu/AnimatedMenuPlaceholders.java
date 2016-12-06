package me.megamichiel.animatedmenu;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.PlayerList;
import com.earth2me.essentials.User;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.megamichiel.animatedmenu.util.RemoteConnections.ServerInfo;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

import static java.util.Locale.ENGLISH;

/**
 * A class that handles some AnimatedMenu created placeholders
 */
public class AnimatedMenuPlaceholders extends PlaceholderHook {
    
    public static void register(AnimatedMenuPlugin plugin) {
        PlaceholderAPI.registerPlaceholderHook(
                "animatedmenu", new AnimatedMenuPlaceholders(plugin));
    }
    
    private final AnimatedMenuPlugin plugin;
    private boolean hasEssentials;
    private IEssentials ess;

    private AnimatedMenuPlaceholders(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
        Plugin p = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (p != null) {
            hasEssentials = true;
            ess = (IEssentials) p;
        }
    }
    
    @Override
    public String onPlaceholderRequest(Player who, String arg) {
        if (arg.startsWith("motd_")) {
            ServerInfo info = plugin.getConnections().get(arg.substring(5));
            if (info == null) return "<invalid>";
            return info.isOnline() ? info.getMotd() : "";
        }
        if (arg.startsWith("onlineplayers_")) {
            ServerInfo info = plugin.getConnections().get(arg.substring(14));
            if (info == null) return "<invalid>";
            return info.isOnline() ? Integer.toString(info.getOnlinePlayers()) : "0";
        }
        if (arg.startsWith("maxplayers_")) {
            ServerInfo info = plugin.getConnections().get(arg.substring(11));
            if (info == null) return "<invalid>";
            return info.isOnline() ? Integer.toString(info.getMaxPlayers()) : "0";
        }
        if (arg.startsWith("status_")) {
            ServerInfo info = plugin.getConnections().get(arg.substring(7).toLowerCase(ENGLISH));
            if (info == null) return "<invalid>";
            IPlaceholder<String> bundle = info.get(info.isOnline() ? "online" : "offline", who);
            return bundle == null ? "<unknown>" : bundle.invoke(plugin, who);
        }
        if (arg.startsWith("motdcheck_")) {
            ServerInfo info = plugin.getConnections().get(arg.substring(10));
            if (info == null) return "<invalid>";
            IPlaceholder<String> value = info.get(
                    info.isOnline() ? info.getMotd().toLowerCase(ENGLISH) : "offline", who);
            if (value == null) value = info.get("default", who);
            return value == null ? "<unknown>" : value.invoke(plugin, who);
        }
        if (arg.startsWith("worldplayers_")) {
            String worldName = arg.substring(13);
            for (World world : Bukkit.getWorlds())
                if (worldName.equalsIgnoreCase(world.getName()))
                    return Integer.toString(world.getPlayers().size());
            return "<invalid>";
        }
        if (arg.startsWith("shownplayers_")) {
            if (!hasEssentials) return "<no_essentials>";
            String worldName = arg.substring(13);
            boolean showHidden = true;
            User user;
            if (who != null) {
                user = ess.getUser(who);
                showHidden = user.isAuthorized("essentials.list.hidden") || user.canInteractVanished();
            } else user = null;
            int count = 0;
            for (List<User> list : PlayerList.getPlayerLists(ess, user, showHidden).values())
                for (User usah : list) if (usah.getWorld().getName().equals(worldName))
                    count++;
            return Integer.toString(count);
        }
        return "<invalid:" + arg + ">";
    }
}
