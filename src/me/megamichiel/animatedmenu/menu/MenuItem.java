package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MenuItem {

    private final MenuItemSettings settings;
    private int frameTick, refreshTick;
    private final int slot;
    
    public MenuItem(MenuItemSettings settings, int slot) {
        this.settings = settings;
        this.slot = slot;
    }

    public MenuItem(AnimatedMenuPlugin plugin, MenuItemSettings settings, Object slot) {
        throw new IllegalArgumentException();
    }

    public void handleMenuClose(Player player) {}
    
    public boolean tick() {
        if (frameTick++ == settings.getFrameDelay())
        {
            frameTick = 0;
            settings.next();
        }
        if (refreshTick++ == settings.getRefreshDelay())
        {
            refreshTick = 0;
            return true;
        }
        return false;
    }

    public int getLastSlot(Player p) {
        return slot;
    }

    public int getSlot(AnimatedMenu menu, Player p) {
        return slot;
    }

    public void apply(Nagger nagger, Player p, ItemStack handle) {
        settings.apply(nagger, p, handle);
    }
    
    public ItemStack load(Nagger nagger, Player p) {
        return settings.first(nagger, p);
    }

    public MenuItemSettings getSettings() {
        return settings;
    }
}
