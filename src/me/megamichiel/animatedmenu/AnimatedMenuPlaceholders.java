package me.megamichiel.animatedmenu;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.PlayerList;
import com.earth2me.essentials.User;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.megamichiel.animatedmenu.util.RemoteConnections.ServerInfo;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * A class that handles some AnimatedMenu created placeholders
 */
public class AnimatedMenuPlaceholders extends PlaceholderHook {
    
    static void register(AnimatedMenuPlugin plugin) {
        PlaceholderAPI.registerPlaceholderHook("animatedmenu", new AnimatedMenuPlaceholders(plugin));
    }
    
    private final AnimatedMenuPlugin plugin;
    private boolean hasEssentials;
    private IEssentials ess;

    private AnimatedMenuPlaceholders(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
        Plugin p = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (p != null) {
            try {
                ess = (IEssentials) p;
                hasEssentials = true;
            } catch (NoClassDefFoundError err) {
                // No essentials
            }
        }
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String arg) {
        int index = arg.indexOf('_');
        switch (index > 0 ? arg.substring(0, index++) : "") { // Increase index by 1 to align with later substrings
            case "motd":
                ServerInfo info = plugin.getConnections().get(arg.substring(index));
                if (info == null) return "<invalid>";
                return info.isOnline() ? info.getMotd() : info.get("offline", player);
            case "onlineplayers":
                info = plugin.getConnections().get(arg.substring(index));
                if (info == null) return "<invalid>";
                return info.isOnline() ? Integer.toString(info.getOnlinePlayers()) : "0";
            case "maxplayers":
                info = plugin.getConnections().get(arg.substring(index));
                if (info == null) return "<invalid>";
                return info.isOnline() ? Integer.toString(info.getMaxPlayers()) : "0";
            case "status":
                info = plugin.getConnections().get(arg.substring(index));
                if (info == null) return "<invalid>";
                String value = info.get(info.isOnline() ? "online" : "offline", player);
                return value == null ? "<unknown>" : value;
            case "motdcheck":
                info = plugin.getConnections().get(arg.substring(index));
                if (info == null) return "<invalid>";
                value = info.get(info.isOnline() ? info.getMotd() : "offline", player);
                if (value == null) value = info.get("default", player);
                return value == null ? "<unknown>" : value;
            case "worldplayers":
                String worldName = arg.substring(index);
                for (World world : Bukkit.getWorlds()) {
                    if (worldName.equalsIgnoreCase(world.getName())) {
                        return Integer.toString(world.getPlayers().size());
                    }
                }
                return "<invalid>";
            case "shownplayers":
                if (!hasEssentials) return "<no_essentials>";
                worldName = arg.substring(index);
                boolean showHidden = true;
                User user;
                if (player != null) {
                    user = ess.getUser(player);
                    showHidden = user.isAuthorized("essentials.list.hidden") || user.canInteractVanished();
                } else user = null;
                int count = 0;
                for (List<User> list : PlayerList.getPlayerLists(ess, user, showHidden).values()) {
                    for (User usah : list) {
                        if (usah.getWorld().getName().equals(worldName)) {
                            count++;
                        }
                    }
                }
                return Integer.toString(count);
            default:
                return "<invalid:" + arg + ">";
        }
    }
}
