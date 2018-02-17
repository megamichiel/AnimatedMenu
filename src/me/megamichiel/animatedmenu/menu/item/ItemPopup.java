package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.menu.MenuSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.function.Function;

public abstract class ItemPopup {

    public static ItemPopup of(Plugin plugin, TimedMenuItem info) {
        return of(plugin, info, () -> info.refresh(IMenuItem.UPDATE_ITEM));
    }

    public static ItemPopup of(Plugin plugin, IMenuItem info, Runnable refresh) {
        return new ItemPopup(plugin) {
            @Override
            protected ItemStack load(Player player, MenuSession session) {
                return info.getItem(player, session, null, -1);
            }

            @Override
            protected void refresh() {
                refresh.run();
            }
        };
    }

    private final MenuSession.Property<State> property = MenuSession.Property.of();

    private final Plugin plugin;

    public ItemPopup(Plugin plugin) {
        this.plugin = plugin;
    }

    protected abstract ItemStack load(Player player, MenuSession session);

    protected abstract void refresh();

    public ItemStack apply(Player player, MenuSession session, ItemStack item, Function<ItemStack, ItemStack> func) {
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
                return load(player, session);
            } else if (state.type != null) {
                return item;
            }
        }
        return item == null ? load(player, session) : func.apply(item);
    }

    public void showError(MenuSession session, String message) {
        show(session, Material.BARRIER, 0, ChatColor.RED + message, 20L);
    }

    public void show(MenuSession session, Material type, String message, long time) {
        show(session, type, 0, message, time);
    }

    public void show(MenuSession session, Material type, int data, String message, long time) {
        State state = session.compute(property, State::new);
        state.update(type, (short) data, message);
        refresh();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            state.update(null, (short) 0, null);
            refresh();
        }, time);
    }

    public boolean canClick(MenuSession session) {
        State state = session.get(property);
        return state == null || state.type == null;
    }

    private class State {

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
