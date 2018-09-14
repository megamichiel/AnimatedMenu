package me.megamichiel.animatedmenu.util.item;

import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MaterialSpecific {

    private static final Map<Class<?>, Class<?>> mapped = new ConcurrentHashMap<>();

    private final Map<Class<?>, Action> actions = new HashMap<>();

    public <IM extends ItemMeta> void add(Class<? extends IM> type, Action<? super IM> action) {
        actions.put(type, action);
    }

    public void apply(Player player, ItemMeta meta, PlaceholderContext context) {
        Class<?> type = mapped.computeIfAbsent(meta.getClass(), a -> {
            for (Class<?> b : actions.keySet()) {
                if (b.isAssignableFrom(a)) {
                    return b;
                }
            }
            return null;
        });
        Action action;
        if (type != null && (action = actions.get(type)) != null) {
            action.apply(player, meta, context);
        }
    }

    public interface Action<IM extends ItemMeta> {
        void apply(Player player, IM meta, PlaceholderContext context);
    }
}
