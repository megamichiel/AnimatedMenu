package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animatedmenu.menu.IMenuItem;
import me.megamichiel.animatedmenu.menu.MenuType;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.config.AbstractConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class MenuItem implements IMenuItem {

    private final MenuItemSettings settings;
    private int frameTick, refreshTick;
    private final int slot;
    
    public MenuItem(AnimatedMenuPlugin plugin, AbstractMenu menu,
                    String key, AbstractConfig section)
            throws IllegalArgumentException {
        MenuType type = menu.getMenuType();
        if (section.isInt("x") && section.isInt("y")) {
            int x = clamp(1, type.getWidth(), section.getInt("x")) - 1,
                    y = clamp(1, type.getHeight(), section.getInt("y")) - 1;
            slot = clamp(1, type.getSize(), (y * type.getWidth()) + x);
        } else if (section.isInt("slot"))
            slot = clamp(1, type.getSize(), section.getInt("slot")) - 1;
        else throw new IllegalArgumentException("No slot specified for item " + key + " in menu " + menu.getName() + "!");
        settings = new MenuItemSettings(plugin, key, menu.getName(), section);
    }

    private static int clamp(int min, int max, int val) {
        return val < min ? min : val > max ? max : val;
    }

    @Override
    public boolean hasDynamicSlot() {
        return false;
    }

    @Override
    public boolean tick() {
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

    @Override
    public int getSlot(Player p, ItemStack[] contents,
                ItemStack stack, boolean updateSlots) {
        return slot;
    }

    @Override
    public void apply(Nagger nagger, Player p, ItemStack handle) {
        settings.apply(nagger, p, handle);
    }

    @Override
    public ItemStack load(Nagger nagger, Player p) {
        return settings.first(nagger, p);
    }

    @Override
    public boolean isHidden(AnimatedMenuPlugin plugin, Player player) {
        return settings.isHidden(plugin, player);
    }

    @Override
    public ItemStack getItem(Nagger nagger, Player player) {
        return settings.getItem(nagger, player);
    }

    @Override
    public void click(Player player, ClickType click) {
        settings.getClickListener().onClick(player, click);
    }
}
