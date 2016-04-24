package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public interface MenuLoader {

    void onEnable(AnimatedMenuPlugin plugin);

    void onDisable();

    List<AnimatedMenu> loadMenus();
}
