package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MenuItem {

    private final MenuItemSettings settings;
    private int frameTick, refreshTick;
    
    public MenuItem(MenuItemSettings settings) {
        this.settings = settings;
    }
    
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
    
    public void apply(Nagger nagger, Player p, ItemStack handle)
    {
        settings.apply(nagger, p, handle);
    }
    
    public ItemStack load(Nagger nagger, Player p) {
        return settings.first(nagger, p);
    }

    public MenuItemSettings getSettings() {
        return settings;
    }
}
