package me.megamichiel.animatedmenu;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.PlayerList;
import com.earth2me.essentials.User;
import me.megamichiel.animatedmenu.util.RemoteConnections;
import me.megamichiel.animationlib.bukkit.placeholder.MVdWPlaceholder;
import me.megamichiel.animationlib.bukkit.placeholder.PapiPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.BiFunction;

/**
 * A class that handles some AnimatedMenu created placeholders
 */
public class AnimatedMenuPlaceholders implements BiFunction<Player, String, String> {

    static void register(AnimatedMenuPlugin plugin, RemoteConnections connections) {
        AnimatedMenuPlaceholders placeholders = new AnimatedMenuPlaceholders(plugin, connections);
        PapiPlaceholder.register(plugin, "animatedmenu", placeholders);
        MVdWPlaceholder.register(plugin, "animatedmenu", placeholders);
    }

    private final RemoteConnections connections;
    
    private boolean hasEssentials;
    private IEssentials ess;

    private AnimatedMenuPlaceholders(AnimatedMenuPlugin plugin, RemoteConnections connections) {
        this.connections = connections;
        
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
    public String apply(Player player, String arg) {
        int index = arg.indexOf('_');
        switch (index > 0 ? arg.substring(0, index++) : "") { // Increase index by 1 to align with later substrings
            case "motd": case "motd1": case "motd2":
                RemoteConnections.IServerInfo info = connections.get(arg.substring(index), true);
                return info == null ? "<invalid>" : info.isOnline() ? index == 5 ? info.getMotd() : (index = (arg = info.getMotd()).indexOf('\n')) >= 0 ?
                        (arg.charAt(4) == '1' ? arg.substring(0, index) : arg) : (arg.charAt(4) == '1' ? arg.substring(index + 1) : "") : info.get("offline", player, "offline");

            case "onlineplayers":
                return (info = connections.get(arg.substring(index), true)) == null ? "<invalid>" : info.isOnline() ? Integer.toString(info.getOnlinePlayers()) : "0";

            case "maxplayers":
                return (info = connections.get(arg.substring(index), true)) == null ? "<invalid>" : info.isOnline() ? Integer.toString(info.getMaxPlayers()) : "0";

            case "status":
                return (info = connections.get(arg.substring(index), false)) == null ? "<invalid>" : info.get(arg = info.isOnline() ? "online" : "offline", player, arg);

            case "motdcheck":
                return (info = connections.get(arg.substring(index), false)) == null ? "<invalid>"
                        : (arg = info.get(info.isOnline() ? info.getMotd() : "offline", player, null)) == null ? info.get("default", player, "default") : arg;

            case "worldplayers":
                World world = Bukkit.getWorld(arg.substring(index));
                return world == null ? "<invalid>" : Integer.toString(world.getPlayers().size());

            case "shownplayers":
                if (!hasEssentials) {
                    return "<no_essentials>";
                }
                String worldName = arg.substring(index);
                User user = player == null ? null : ess.getUser(player);

                return Long.toString(PlayerList.getPlayerLists(ess, user,
                        user == null || user.isAuthorized("essentials.list.hidden") || user.canInteractVanished()
                ).values().stream().flatMap(List::stream).filter($user -> $user.getWorld().getName().equals(worldName)).count());

            default:
                return "<invalid:" + arg + ">";
        }
    }
}
