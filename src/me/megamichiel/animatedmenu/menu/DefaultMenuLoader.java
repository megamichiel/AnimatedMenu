package me.megamichiel.animatedmenu.menu;

import com.google.common.base.Predicate;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.MenuRegistry;
import me.megamichiel.animatedmenu.animation.AnimatedMaterial;
import me.megamichiel.animatedmenu.command.SoundCommand;
import me.megamichiel.animatedmenu.util.DirectoryListener;
import me.megamichiel.animatedmenu.util.DirectoryListener.FileAction;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animationlib.YamlConfig;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultMenuLoader implements MenuLoader, DirectoryListener.FileListener {

    private DirectoryListener listener;
    protected AnimatedMenuPlugin plugin;

    @Override
    public void onEnable(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
        File menus = new File(plugin.getDataFolder(), "menus");
        if (!menus.exists()) {
            menus.mkdir();
            plugin.saveResource("menus/example.yml", false);
            plugin.saveResource("menus/shop.yml", false);
        }
        if (plugin.getConfig().getBoolean("auto-menu-refresh")) {
            try {
                listener = new DirectoryListener(plugin.getLogger(), new File(plugin.getDataFolder(), "menus"), this);
            } catch (IOException ex) {
                plugin.nag("Unable to set up directory update listener!");
                plugin.nag(ex);
            }
        }
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.stop();
        }
    }

    @Override
    public List<AnimatedMenu> loadMenus() {
        File menus = new File(plugin.getDataFolder(), "menus");

        List<AnimatedMenu> list = new ArrayList<>();

        loadMenus(list, menus);

        return list;
    }

    protected void loadMenus(List<AnimatedMenu> menus, File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                loadMenus(menus, file);
                continue;
            }
            String name = file.getName(), extension = name.substring(name.lastIndexOf('.') + 1);
            if (extension.equalsIgnoreCase("yml")) {
                name = name.substring(0, name.lastIndexOf('.')).replace(' ', '-');
                FileConfiguration config = YamlConfig.loadConfig(file);
                AnimatedMenu menu = loadMenu(name, config);
                menus.add(menu);
            }
        }
    }

    protected AnimatedMenu loadMenu(String name, ConfigurationSection config) {
        AnimatedText title = new AnimatedText();
        title.load(plugin, config, "menu-name", new StringBundle(plugin, name));
        MenuType type;
        if (config.isSet("menu-type")) {
            type = MenuType.fromName(config.getString("menu-type").toUpperCase());
            if(type == null) {
                plugin.nag("Couldn't find a menu type by name " + config.getString("menu-type") + "!");
                type = MenuType.chest(6);
            }
        } else {
            type = MenuType.chest(config.getInt("rows", 6));
        }
        AnimatedMenu menu = type.newMenu(plugin, name, title, config.getInt("title-update-delay", 20));
        loadMenu(menu, config);
        return menu;
    }

    protected void loadMenu(AnimatedMenu menu, ConfigurationSection config) {
        loadSettings(menu.getSettings(), config);
        ConfigurationSection items = config.getConfigurationSection("items");
        if (items == null) {
            plugin.nag("No items specified for " + menu.getName() + "!");
            return;
        }
        MenuType type = menu.getMenuType();
        for (String key : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(key);
            MenuItemSettings settings = new MenuItemSettings(plugin, key, menu.getName(), section);
            MenuItem item;
            if (section.isInt("x") && section.isInt("y")) {
                int x = clamp(1, type.getWidth(), section.getInt("x")) - 1,
                        y = clamp(1, type.getHeight(), section.getInt("y")) - 1;
                item = new MenuItem(settings, (y * type.getWidth()) + x);
            } else if (section.isInt("slot")) {
                item = new MenuItem(settings, clamp(1, type.getSize(), section.getInt("slot")) - 1);
            } else if (section.isSet("slot")) {
                try {
                    item = new MenuItem(plugin, settings, section.get("slot"));
                } catch (IllegalArgumentException ex) {
                    plugin.nag("Invalid slot specified for item " + key + " in menu " + menu.getName() + "!");
                    continue;
                }
            } else {
                plugin.nag("No slot specified for item " + key + " in menu " + menu.getName() + "!");
                continue;
            }
            menu.getMenuGrid().addItem(item);
        }
    }

    protected void loadSettings(MenuSettings settings, ConfigurationSection section) {
        if (section.isSet("menu-opener")) {
            ItemStack opener = AnimatedMaterial.parseItemStack(plugin, section.getString("menu-opener"));
            boolean openerName = section.isSet("menu-opener-name"),
                    openerLore = section.isSet("menu-opener-lore");
            if (openerName || openerLore) {
                ItemMeta meta = opener.getItemMeta();
                if(openerName)
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("menu-opener-name")));
                if(openerLore) {
                    List<String> lore = new ArrayList<>();
                    for (String line : section.getStringList("menu-opener-lore"))
                        lore.add(ChatColor.translateAlternateColorCodes('&', line));
                    meta.setLore(lore);
                }
                opener.setItemMeta(meta);
            }
            settings.setOpener(opener);
            settings.setOpenerJoinSlot(section.getInt("menu-opener-slot", -1));
        }
        settings.setOpenOnJoin(Flag.parseBoolean(section, "open-on-join", false));
        if (section.isSet("open-sound")) {
            final SoundCommand.SoundInfo sound = new SoundCommand.SoundInfo(plugin,
                    section.getString("open-sound").toLowerCase().replace('-', '_'),
                    1F, (float) section.getDouble("open-sound-pitch", 1F)
            );
            settings.addOpenListener(new Predicate<Player>() {
                @Override
                public boolean apply(@Nullable Player player) {
                    sound.play(player);
                    return false;
                }
            });
        }
        settings.setOpenCommands(section.contains("command") ? section.getString("command").toLowerCase().split("; ") : null);
        settings.setHiddenFromCommand(section.getBoolean("hide-from-command"));
    }

    private int clamp(int min, int max, int val) {
        return val < min ? min : val > max ? max : val;
    }

    @Override
    public void fileChanged(File file, FileAction action) {
        if (file.isFile() || action == FileAction.DELETE)
        {
            int index = file.getName().lastIndexOf('.');
            if (index == -1) return;
            if (!file.toPath().startsWith(new File(plugin.getDataFolder(), "menus").toPath())) return;
            String extension = file.getName().substring(index + 1);
            if (extension.equals("yml"))
            {
                String name = file.getName().substring(0, index).replace(" ", "_");
                MenuRegistry registry = plugin.getMenuRegistry();
                switch (action)
                {
                    case CREATE:
                        AnimatedMenu menu = registry.getMenu(name);
                        if (menu != null) {
                            plugin.getLogger().warning("A new menu file was created," +
                                    " but a menu already existed with it's name!");
                            Collection<? extends Player> viewers = menu.getViewers();
                            for (Player player : viewers)
                                player.closeInventory();
                            registry.remove(menu);
                        }
                        FileConfiguration cfg = YamlConfig.loadConfig(file);
                        menu = loadMenu(name, cfg);
                        plugin.getLogger().info("Loaded " + file.getName());
                        registry.add(menu);
                        break;
                    case MODIFY:
                        menu = registry.getMenu(name);
                        if (menu == null) {
                            plugin.getLogger().warning("Failed to update " + name + ": No menu by that name found!");
                            return;
                        }
                        if (file.length() == 0) return;
                        Collection<? extends Player> viewers = menu.getViewers();
                        for (Player player : viewers)
                            player.closeInventory();
                        cfg = YamlConfig.loadConfig(file);
                        registry.remove(menu);
                        menu = loadMenu(name, cfg);
                        registry.add(menu);
                        for (Player player : viewers)
                            registry.openMenu(player, menu);
                        plugin.getLogger().info("Updated " + file.getName());
                        break;
                    case DELETE:
                        menu = registry.getMenu(name);
                        if (menu == null) {
                            plugin.getLogger().warning("Failed to remove " + name + ": No menu by that name found!");
                            return;
                        }
                        viewers = menu.getViewers();
                        for (Player player : viewers)
                            player.closeInventory();
                        registry.remove(menu);
                        plugin.getLogger().info("Removed " + file.getName());
                        break;
                }
            }
        }
    }
}
