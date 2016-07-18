package me.megamichiel.animatedmenu;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.megamichiel.animatedmenu.util.FormulaPlaceholder;
import me.megamichiel.animatedmenu.util.RemoteConnections.ServerInfo;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * A class that handles some AnimatedMenu created placeholders
 */
public class AnimatedMenuPlaceholders extends PlaceholderHook {
    
    public static void register(AnimatedMenuPlugin plugin) {
        AnimatedMenuPlaceholders instance = new AnimatedMenuPlaceholders(plugin);
        PlaceholderAPI.registerPlaceholderHook("animatedmenu", instance);
    }
    
    private final AnimatedMenuPlugin plugin;

    AnimatedMenuPlaceholders(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String onPlaceholderRequest(Player who, String arg) {
        String lower = arg.toLowerCase(Locale.US);
        if (lower.startsWith("motd_")) {
            ServerInfo info = plugin.getConnections().get(lower.substring(5));
            if (info == null) return "<invalid>";
            return info.isOnline() ? info.getMotd() : "";
        }
        if (lower.startsWith("onlineplayers_")) {
            ServerInfo info = plugin.getConnections().get(lower.substring(14));
            if (info == null) return "<invalid>";
            return info.isOnline() ? Integer.toString(info.getOnlinePlayers()) : "0";
        }
        if (lower.startsWith("maxplayers_")) {
            ServerInfo info = plugin.getConnections().get(lower.substring(11));
            if (info == null) return "<invalid>";
            return info.isOnline() ? Integer.toString(info.getOnlinePlayers()) : "0";
        }
        if (lower.startsWith("status_")) {
            ServerInfo info = plugin.getConnections().get(lower.substring(7).toLowerCase(Locale.US));
            if (info == null) return "<invalid>";
            IPlaceholder<String> bundle = info.get(info.isOnline() ? "online" : "offline", who);
            return bundle == null ? "<unknown>" : bundle.invoke(plugin, who);
        }
        if (lower.startsWith("motdcheck_")) {
            ServerInfo info = plugin.getConnections().get(lower.substring(10));
            if (info == null) return "<invalid>";
            IPlaceholder<String> value = info.get(
                    info.isOnline() ? info.getMotd().toLowerCase(Locale.US) : "offline", who);
            if (value == null) value = info.get("default", who);
            return value == null ? "<unknown>" : value.invoke(plugin, who);
        }
        if (lower.startsWith("worldplayers_")) {
            String worldName = lower.substring(13);
            for (World world : Bukkit.getWorlds())
                if (worldName.equalsIgnoreCase(world.getName()))
                    return Integer.toString(world.getPlayers().size());
            return "<invalid>";
        }
        if (lower.startsWith("formula_")) {
            FormulaPlaceholder ph = plugin.getFormulaPlaceholders().get(lower.substring(8));
            if (ph != null)
                return ph.invoke(plugin, who).toString();
            return "<invalid>";
        }
        return "<invalid:" + arg + ">";
    }
}
