package me.megamichiel.animatedmenu.menu;

import com.google.common.base.Supplier;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MenuItem {

    private final MenuItemSettings settings;
    private int frameTick, refreshTick;
    private final int slot;
    
    public MenuItem(AbstractMenu menu, MenuItemSettings settings, int slot) {
        this.settings = settings;
        this.slot = slot;
    }

    MenuItem(AbstractMenu menu, MenuItemSettings settings, Object slot) {
        throw new IllegalArgumentException();
    }

    boolean hasDynamicSlot() {
        return false;
    }
    
    boolean tick() {
        if (frameTick++ == settings.getFrameDelay()) {
            frameTick = 0;
            settings.next();
        }
        if (refreshTick++ == settings.getRefreshDelay()) {
            refreshTick = 0;
            return true;
        }
        return false;
    }

    int getSlot(Player p, ItemStack[] contents) {
        return slot;
    }

    public void apply(Nagger nagger, Player p, ItemStack handle) {
        settings.apply(nagger, p, handle);
    }
    
    public ItemStack load(Nagger nagger, Player p) {
        return settings.first(nagger, p);
    }

    MenuItemSettings getSettings() {
        return settings;
    }
}
