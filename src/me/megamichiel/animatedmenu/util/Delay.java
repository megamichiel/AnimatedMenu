package me.megamichiel.animatedmenu.util;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.placeholder.ctx.ParsingContext;
import me.megamichiel.animationlib.util.ParsingNagger;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Delay implements Runnable, ParsingNagger, ParsingContext {

    private final Nagger nagger;
    private final String id;

    private final Map<UUID, Long> delays = new HashMap<>();
    private final StringBundle delayMessage;
    private final long delay;
    private long result;

    public Delay(AnimatedMenuPlugin plugin, String id, String delayMessage, long delay) {
        nagger = plugin;
        this.id = id;

        this.delayMessage = delayMessage == null ? null : StringBundle.parse(this, delayMessage).colorAmpersands();
        this.delay = delay;

        // Remove expired delays every minute to keep the map at a reasonable size
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, 1200L, 1200L);
    }

    public long getDelay() {
        return delay;
    }

    public String getId() {
        return id;
    }

    public Map<UUID, Long> save() {
        if (delays.isEmpty()) {
            return Collections.emptyMap();
        }
        long time = System.currentTimeMillis(), delay;
        Map<UUID, Long> save = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : delays.entrySet()) {
            if (time < (delay = entry.getValue())) {
                save.put(entry.getKey(), delay);
            }
        }
        return save;
    }

    public void load(Map<UUID, Long> map) {
        delays.putAll(map);
    }

    public boolean test(Player player) {
        long time = System.currentTimeMillis();

        AtomicBoolean bool = new AtomicBoolean();
        delays.compute(player.getUniqueId(), (id, timer) -> {
            if (timer == null || time >= timer) {
                bool.set(true);
                return time + delay;
            } else if (delayMessage != null) {
                result = timer - time;
                player.sendMessage(delayMessage.toString(player));
            }
            return timer;
        });

        return bool.get();
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        delays.values().removeIf(timer -> time >= timer);
    }

    @Override
    public ParsingContext context() {
        return this;
    }

    @Override
    public ParsingContext parent() {
        return nagger instanceof ParsingNagger ? ((ParsingNagger) nagger).context() : null;
    }

    @Override
    public void nag(String s) {
        nagger.nag(s);
    }

    @Override
    public void nag(Throwable throwable) {
        nagger.nag(throwable);
    }

    @Override
    public IPlaceholder<?> parse(String s) {
        switch (s) {
            case "delay_ticks_total":
                return (n, p) -> result /         50;
            case "delay_ticks": case "ticksleft":
                return (n, p) -> result /         50 % 20;
            case "delay_seconds_total":
                return (n, p) -> result /      1_000;
            case "delay_seconds": case "secondsleft":
                return (n, p) -> result /      1_000 % 60;
            case "delay_minutes_total":
                return (n, p) -> result /     60_000;
            case "delay_minutes": case "minutesleft":
                return (n, p) -> result /     60_000 % 60;
            case "delay_hours_total": case "hoursleft":
                return (n, p) -> result /   3600_000;
            case "delay_hours":
                return (n, p) -> result /   3600_000 % 24;
            case "delay_days_total":
                return (n, p) -> result /  86400_000;
            case "delay_days":
                return (n, p) -> result /  86400_000 % 7;
            case "delay_weeks":
                return (n, p) -> result / 604800_000;
        }
        return null;
    }
}
