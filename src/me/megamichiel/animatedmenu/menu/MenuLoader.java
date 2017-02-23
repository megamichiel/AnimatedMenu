package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.MenuRegistry;
import me.megamichiel.animatedmenu.animation.AnimatedMaterial;
import me.megamichiel.animatedmenu.command.SoundCommand;
import me.megamichiel.animatedmenu.menu.item.ClickHandler;
import me.megamichiel.animatedmenu.menu.item.ConfigItemInfo;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MenuLoader implements DirectoryListener.FileListener {

    private DirectoryListener listener;
    protected final AnimatedMenuPlugin plugin;
    private final File directory;

    public MenuLoader(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
        File menus = directory = new File(plugin.getDataFolder(), "menus");
        if (!menus.exists()) {
            if (menus.mkdir()) {
                plugin.saveResource("menus/example.yml", false);
                plugin.saveResource("menus/shop.yml", false);
            } else plugin.nag("Failed to create menus folder!");
        }
    }

    public void onDisable() {
        if (listener != null) listener.stop();
    }

    public List<AnimatedMenu> loadMenus() {
        List<AnimatedMenu> list = new ArrayList<>();

        loadMenus(list, directory);

        if (listener != null) {
            listener.stop();
            listener = null;
        }

        if (Flag.parseBoolean(plugin.getConfiguration().getString("auto-menu-refresh"))) {
            try {
                listener = new DirectoryListener(plugin.getLogger(), directory, this);
            } catch (IOException ex) {
                plugin.nag("Unable to set up directory update listener!");
                plugin.nag(ex);
            }
        }

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
            String name = file.getName(), extension;
            int index = name.lastIndexOf('.');
            if (index == -1) continue;
            extension = name.substring(index + 1);
            if (extension.equalsIgnoreCase("yml")) {
                menus.add(loadMenu(name.substring(0, index).replace(' ', '-'),
                        ConfigManager.quickLoad(YamlConfig::new, file)));
            }
        }
    }

    protected AnimatedMenu createMenu(AnimatedMenuPlugin plugin, String name,
                                      AnimatedText title, int titleUpdateDelay,
                                      MenuType type, StringBundle permission,
                                      StringBundle permissionMessage) {
        return new AnimatedMenu(plugin, name, this, title,
                titleUpdateDelay, type, permission, permissionMessage);
    }

    public AnimatedMenu loadMenu(String name, AbstractConfig config) {
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
        AnimatedMenu menu = createMenu(plugin, name, title, config.getInt(
                "title-update-delay", 20), type, permission, permissionMessage);
        loadMenu(menu, config);
        return menu;
    }

    protected void loadMenu(AnimatedMenu menu, AbstractConfig config) {
        loadSettings(menu.getSettings(), config);
        menu.setClickDelay(config.getString("delay-message"), config.getLong("click-delay") * 50L);
        Map<String, AbstractConfig> items = new HashMap<>();
        if (config.isSection("items")) {
            AbstractConfig cfg = config.getSection("items");
            cfg.values().forEach((key, value) -> {
                if (value instanceof AbstractConfig)
                    items.put(key, (AbstractConfig) value);
            });
        } else if (config.isList("items")) {
            List<AbstractConfig> list = config.getSectionList("items");
            for (int i = 0; i < list.size(); i++)
                items.put(Integer.toString(i + 1), list.get(i));
        }
        if (items.isEmpty() && !menu.hasEmptyItem()) {
            plugin.nag("No items specified for " + menu.getName() + "!");
            return;
        }
        items.forEach((key, value) -> loadItem(menu, key, value, true, menu.getMenuGrid()::add));
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
            settings.setOpener(opener, section.getInt("menu-opener-slot", -1));
        }
        settings.setOpenOnJoin(Flag.parseBoolean(section.getString("open-on-join")));
        if (section.isString("open-sound")) {
            SoundCommand.SoundInfo sound = new SoundCommand.SoundInfo(plugin,
                    section.getString("open-sound").toLowerCase(Locale.US).replace('-', '_'),
                    1F, (float) section.getDouble("open-sound-pitch", 1F));
            settings.addListener(sound::play, false);
        }
        settings.setHiddenFromCommand(Flag.parseBoolean(section.getString("hide-from-command")));

        Object command = section.get("command");
        if (command != null) {
            AnimatedMenu menu = settings.getMenu();
            String name, usage = menu.getDefaultUsage(), description = "Opens menu " + menu.getName();
            if (command instanceof AbstractConfig) {
                AbstractConfig sec = (AbstractConfig) command;
                name = sec.getString("name");
                usage = sec.getString("usage", usage);
                description = sec.getString("description", description);
            } else if (command instanceof String || AbstractConfig.isPrimitiveWrapper(command))
                name = command.toString();
            else name = null;

            if (name != null) {
                String[] split = name.split(";");
                name = split[0].trim().toLowerCase(Locale.ENGLISH);
                String[] aliases = new String[split.length - 1];
                for (int i = 0; i < aliases.length; i++)
                    aliases[i] = split[i + 1].trim().toLowerCase(Locale.ENGLISH);

                settings.setOpenCommand(name, usage, description, aliases);
            }
        }
    }

    public void loadItem(AbstractMenu menu, String name, AbstractConfig section,
                         boolean withSlot, Consumer<ItemInfo> action) {
        try {
            action.accept(new ConfigItemInfo(plugin, menu, name, section));
        } catch (IllegalArgumentException ex) {
            plugin.nag(ex.getMessage());
        }
    }

    public ClickHandler.Entry parseClickHandler(String path, AbstractConfig section) {
        try {
            return new ClickHandler.Entry(plugin, section);
        } catch (IllegalArgumentException ex) {
            plugin.nag(ex.getMessage() + " in " + path);
            return null;
        }
    }

    @Override
    public void fileChanged(File file, FileAction action) {
        if (file.isFile() || action == FileAction.DELETE) {
            int index = file.getName().lastIndexOf('.');
            if (index == -1) return;
            if (!file.toPath().startsWith(directory.toPath())) return;
            String extension = file.getName().substring(index + 1);
            if (extension.equals("yml")) {
                String name = file.getName().substring(0, index).replace(" ", "_");
                MenuRegistry registry = plugin.getMenuRegistry();
                switch (action) {
                    case CREATE:
                        AnimatedMenu menu = registry.getMenu(name);
                        if (menu != null) {
                            plugin.getLogger().warning("A new menu file was created," +
                                    " but a menu already existed with its name!");

                            menu.closeAll();
                            registry.remove(menu);
                        }
                        menu = loadMenu(name, ConfigManager.quickLoad(YamlConfig::new, file));
                        plugin.getLogger().info("Loaded " + file.getName());
                        registry.add(menu);
                        break;
                    case MODIFY:
                        if (file.length() == 0) return;
                        menu = registry.getMenu(name);
                        Set<Player> viewers;
                        if (menu != null) {
                            viewers = menu.getViewers();
                            for (Object viewer : viewers)
                                ((Player) viewer).closeInventory();
                            registry.remove(menu);
                        } else viewers = Collections.emptySet();
                        menu = loadMenu(name, ConfigManager.quickLoad(YamlConfig::new, file));
                        registry.add(menu);
                        for (Player p : viewers)
                            registry.openMenu(p, menu);
                        plugin.getLogger().info("Updated " + file.getName());
                        break;
                    case DELETE:
                        menu = registry.getMenu(name);
                        if (menu == null) {
                            plugin.getLogger().warning("Failed to remove " + name + ": No menu by that name found!");
                            return;
                        }
                        menu.closeAll();
                        registry.remove(menu);
                        plugin.getLogger().info("Removed " + file.getName());
                        break;
                }
            }
        }
    }
}
