package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.MenuRegistry;
import me.megamichiel.animatedmenu.animation.AnimatedMaterial;
import me.megamichiel.animatedmenu.command.SoundCommand;
import me.megamichiel.animatedmenu.menu.item.MenuItem;
import me.megamichiel.animatedmenu.util.DirectoryListener;
import me.megamichiel.animatedmenu.util.DirectoryListener.FileAction;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MenuLoader implements DirectoryListener.FileListener {

    private DirectoryListener listener;
    protected AnimatedMenuPlugin plugin;

    public void onEnable(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
        File menus = new File(plugin.getDataFolder(), "menus");
        if (!menus.exists()) {
            if (menus.mkdir()) {
                plugin.saveResource("menus/example.yml", false);
                plugin.saveResource("menus/shop.yml", false);
            } else {
                plugin.nag("Failed to create menus folder!");
            }
        }
        if (plugin.getConfiguration().getBoolean("auto-menu-refresh")) {
            try {
                listener = new DirectoryListener(plugin.getLogger(), new File(plugin.getDataFolder(), "menus"), this);
            } catch (IOException ex) {
                plugin.nag("Unable to set up directory update listener!");
                plugin.nag(ex);
            }
        }
    }

    public void onDisable() {
        if (listener != null) listener.stop();
    }

    public List<AnimatedMenu> loadMenus() {
        File menus = new File(plugin.getDataFolder(), "menus");

        List<AnimatedMenu> list = new ArrayList<>();

        loadMenus(list, menus);

        return list;
    }

    private void loadMenus(List<AnimatedMenu> menus, File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                loadMenus(menus, file);
                continue;
            }
            String name = file.getName(), extension = name.substring(name.lastIndexOf('.') + 1);
            if (extension.equalsIgnoreCase("yml")) {
                name = name.substring(0, name.lastIndexOf('.')).replace(' ', '-');
                YamlConfig config = ConfigManager.quickLoad(YamlConfig::new, file);
                AnimatedMenu menu = loadMenu(name, config);
                menus.add(menu);
            }
        }
    }

    protected AnimatedMenu loadMenu(String name, AbstractConfig config) {
        AnimatedText title = new AnimatedText();
        title.load(plugin, config, "menu-name", new StringBundle(plugin, name));
        MenuType type;
        if (config.isSet("menu-type") && !"chest".equalsIgnoreCase(config.getString("menu-type"))) {
            type = MenuType.fromName(config.getString("menu-type").toUpperCase());
            if(type == null) {
                plugin.nag("Couldn't find a menu type by name " + config.getString("menu-type") + "!");
                type = MenuType.chest(6);
            }
        } else type = MenuType.chest(config.getInt("rows", 6));
        StringBundle permission = StringBundle.parse(plugin, config.getString("permission")),
                permissionMessage = StringBundle.parse(plugin, config.getString("permission-message"));
        if (permissionMessage != null) permissionMessage.colorAmpersands();
        AnimatedMenu menu = type.newMenu(plugin, name, this, title,
                config.getInt("title-update-delay", 20), permission, permissionMessage);
        loadMenu(menu, config);
        return menu;
    }

    private void loadMenu(AnimatedMenu menu, AbstractConfig config) {
        loadSettings(menu.getSettings(), config);
        AbstractConfig items = config.getSection("items");
        if (items == null) {
            plugin.nag("No items specified for " + menu.getName() + "!");
            return;
        }
        for (String key : items.keys()) {
            AbstractConfig section = items.getSection(key);
            IMenuItem item = createItem(menu, key,
                    items.getSection(key));
            if (item != null) menu.getMenuGrid().addItem(item);
        }
        menu.getMenuGrid().sortSlots(); // Put items with non-dynamic slot before those with it
    }

    protected void loadSettings(MenuSettings settings, AbstractConfig section) {
        if (section.isSet("menu-opener")) {
            ItemStack opener = AnimatedMaterial.parseItemStack(plugin, section.getString("menu-opener"));
            boolean openerName = section.isSet("menu-opener-name"),
                    openerLore = section.isSet("menu-opener-lore");
            if (openerName || openerLore) {
                ItemMeta meta = opener.getItemMeta();
                if (openerName)
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("menu-opener-name")));
                if (openerLore) {
                    meta.setLore(section.getStringList("menu-opener-lore").stream()
                            .map(StringBundle::colorAmpersands).collect(Collectors.toList()));
                }
                opener.setItemMeta(meta);
            }
            settings.setOpener(opener);
            settings.setOpenerJoinSlot(section.getInt("menu-opener-slot", -1));
        }
        settings.setOpenOnJoin(Flag.parseBoolean(section.getString("open-on-join"), false));
        if (section.isSet("open-sound")) {
            final SoundCommand.SoundInfo sound = new SoundCommand.SoundInfo(plugin,
                    section.getString("open-sound").toLowerCase(Locale.US).replace('-', '_'),
                    1F, (float) section.getDouble("open-sound-pitch", 1F)
            );
            settings.addOpenListener(sound::play);
        }
        settings.setOpenCommands(section.isString("command") ? section.getString("command").toLowerCase(Locale.US).split("; ") : null);
        settings.setHiddenFromCommand(section.getBoolean("hide-from-command"));
    }

    public IMenuItem createItem(AbstractMenu menu,
                                String name, AbstractConfig section) {
        try {
            return new MenuItem(plugin, menu, name, section);
        } catch (IllegalArgumentException ex) {
            plugin.nag(ex.getMessage());
            return null;
        }
    }

    @Override
    public void fileChanged(File file, FileAction action) {
        if (file.isFile() || action == FileAction.DELETE) {
            int index = file.getName().lastIndexOf('.');
            if (index == -1) return;
            if (!file.toPath().startsWith(new File(plugin.getDataFolder(), "menus").toPath())) return;
            String extension = file.getName().substring(index + 1);
            if (extension.equals("yml")) {
                String name = file.getName().substring(0, index).replace(" ", "_");
                MenuRegistry registry = plugin.getMenuRegistry();
                switch (action) {
                    case CREATE:
                        AnimatedMenu menu = registry.getMenu(name);
                        if (menu != null) {
                            plugin.getLogger().warning("A new menu file was created," +
                                    " but a menu already existed with it's name!");
                            for (Iterator<Map.Entry<Player, Inventory>> it = menu.getViewers(); it.hasNext();)
                                it.next().getKey().closeInventory();
                            registry.remove(menu);
                        }
                        YamlConfig config = ConfigManager.quickLoad(YamlConfig::new, file);
                        menu = loadMenu(name, config);
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
                        for (Iterator<Map.Entry<Player, Inventory>> it = menu.getViewers(); it.hasNext();)
                            it.next().getKey().closeInventory();
                        config = ConfigManager.quickLoad(YamlConfig::new, file);
                        registry.remove(menu);
                        menu = loadMenu(name, config);
                        registry.add(menu);
                        for (Iterator<Map.Entry<Player, Inventory>> it = menu.getViewers(); it.hasNext();)
                            it.next().getKey().closeInventory();
                        plugin.getLogger().info("Updated " + file.getName());
                        break;
                    case DELETE:
                        menu = registry.getMenu(name);
                        if (menu == null) {
                            plugin.getLogger().warning("Failed to remove " + name + ": No menu by that name found!");
                            return;
                        }
                        for (Iterator<Map.Entry<Player, Inventory>> it = menu.getViewers(); it.hasNext();)
                            it.next().getKey().closeInventory();
                        registry.remove(menu);
                        plugin.getLogger().info("Removed " + file.getName());
                        break;
                }
            }
        }
    }
}
