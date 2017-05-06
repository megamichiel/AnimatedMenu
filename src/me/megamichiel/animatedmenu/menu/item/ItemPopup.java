package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.menu.MenuItem;
import me.megamichiel.animatedmenu.menu.MenuSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.function.Function;

public class ItemPopup {

    private final MenuSession.Property<State> property = MenuSession.Property.of();

    private Plugin plugin;
    private MenuItem item;
    private ItemInfo info;

    public void init(Plugin plugin, MenuItem item) {
        this.plugin = plugin;
        this.item = item;
        this.info = item.getInfo();
    }

    public ItemStack apply(MenuSession session, ItemStack item, Function<ItemStack, ItemStack> func) {
        State state = session.get(property);
        if (state != null) {
            if (state.changed) {
                state.changed = false;
                if (state.type != null) {
                    ItemMeta meta = (item = new ItemStack(state.type, 1, state.data)).getItemMeta();
                    meta.setDisplayName(state.message);
                    item.setItemMeta(meta);
                    return item;
                }
                return info.load(session.getPlayer(), session);
            } else if (state.type != null) {
                return item;
            }
        }
        return func.apply(item);
    }

    public void showError(MenuSession session, String message) {
        show(session, Material.BARRIER, 0, ChatColor.RED + message, 20L);
    }

    public void show(MenuSession session, Material type, String message, long time) {
        show(session, type, 0, message, time);
    }

    public void show(MenuSession session, Material type, int data, String message, long time) {
        State state = session.compute(property, State::new);
        state.type = type;
        state.data = (short) data;
        state.message = message;
        state.changed = true;
        item.requestUpdate(ItemInfo.DelayType.REFRESH);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            state.type = null;
            state.message = null;
            state.changed = true;
            item.requestUpdate(ItemInfo.DelayType.REFRESH);
        }, time);
    }

    public boolean canClick(MenuSession session) {
        State state = session.get(property);
        return state == null || state.type == null;
    }

    private static class State {

        private Material type;
        private short data;
        private String message;
        private boolean changed;

        void update(Material type, short data, String message) {
            this.type = type;
            this.data = data;
            this.message = message;
            changed = true;
        }
    }
}
