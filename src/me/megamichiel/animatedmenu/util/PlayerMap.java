package me.megamichiel.animatedmenu.util;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ConcurrentHashMap;

public class PlayerMap<V> extends ConcurrentHashMap<Player, V> implements Listener {

    public PlayerMap() {}

    public PlayerMap(Plugin plugin) {
        init(plugin);
    }

    public void init(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    void on(PlayerQuitEvent e) {
        remove(e.getPlayer());
    }
}
