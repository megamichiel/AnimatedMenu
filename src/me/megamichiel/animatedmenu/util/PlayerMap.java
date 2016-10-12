package me.megamichiel.animatedmenu.util;

import me.megamichiel.animationlib.bukkit.PipelineListener;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerMap<V> extends ConcurrentHashMap<Player, V> implements Listener {

    public PlayerMap() {}

    public PlayerMap(Plugin plugin) {
        init(plugin);
    }

    public void init(Plugin plugin) {
        PipelineListener.newPipeline(PlayerQuitEvent.class, plugin)
                .map(PlayerEvent::getPlayer).forEach(this::remove);
    }
}
