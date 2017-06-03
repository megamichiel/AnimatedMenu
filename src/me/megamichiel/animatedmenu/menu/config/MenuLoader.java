package me.megamichiel.animatedmenu.menu.config;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.OpenAnimation;
import me.megamichiel.animatedmenu.command.SoundCommand;
import me.megamichiel.animatedmenu.menu.*;
import me.megamichiel.animatedmenu.menu.item.ItemInfo;
import me.megamichiel.animatedmenu.util.DirectoryListener;
import me.megamichiel.animatedmenu.util.DirectoryListener.FileAction;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animationlib.animation.AnimatedText;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.config.ConfigException;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MenuLoader implements DirectoryListener.FileListener {

    private DirectoryListener listener;
    protected final AnimatedMenuPlugin plugin;
    private final File directory;

    private List<AnimatedMenu> customMenus;

    public MenuLoader(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
        File menus = directory = new File(plugin.getDataFolder(), "menus");
        if (!menus.exists()) {
            if (menus.mkdirs()) {
                saveDefaultMenus();
            } else plugin.nag("Failed to create menus folder!");
        }
    }

    protected void saveDefaultMenus() {
        plugin.saveResource("menus/example.yml", false);
    }

    public void onDisable() {
        if (listener != null) listener.stop();
        if (customMenus != null) {
            customMenus.forEach(plugin.getMenuRegistry()::remove);
            customMenus = null;
        }
    }

    public void loadConfig() {
        MenuRegistry menuRegistry = plugin.getMenuRegistry();

        Logger logger = plugin.getLogger();
        logger.info("Loading menus...");

        File file = new File(plugin.getDataFolder(), "images");
        if (!file.exists() && !file.mkdir()) {
            logger.warning("Failed to create images folder!");
        }

        List<AnimatedMenu> list = customMenus = loadMenus();
        if (list != null) {
            list.forEach(menuRegistry::add);
        }
        int size = menuRegistry.getMenus().size();
        logger.info(size + " menu" + (size == 1 ? "" : "s") + " loaded");
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
            String name = file.getName();
            int index = name.lastIndexOf('.');
            if (index == -1) continue;
            if (name.substring(index + 1).equalsIgnoreCase("yml")) {
                YamlConfig config;
                try {
                    config = ConfigManager.quickLoad(YamlConfig::new, file);
                } catch (ConfigException ex) {
                    plugin.nag("Failed to load menu file " + file.getName() + "!");
                    plugin.nag(ex);
                    continue;
                }
                menus.add(loadMenu(name.substring(0, index).replace(' ', '-'), config));
            }
        }
    }

    protected AnimatedMenu createMenu(String name, AbstractConfig config,
                                      AnimatedText title, int titleUpdateDelay, MenuType type) {
        return new AnimatedMenu(name, title, titleUpdateDelay, type);
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
        AnimatedMenu menu = createMenu(name, config, title, plugin.parseTime(config, "title-update-delay", 20), type);

        StringBundle permission = StringBundle.parse(plugin, config.getString("permission"));
        if (permission != null) {
            menu.getSettings().setPermission(permission.tryCache());
            StringBundle message = StringBundle.parse(plugin, config.getString("permission-message"));
            if (message != null) {
                menu.getSettings().setPermissionMessage(message.colorAmpersands().tryCache());
            }
        }

        loadMenu(menu, config);
        return menu;
    }

    protected void loadMenu(AnimatedMenu menu, AbstractConfig config) {
        loadSettings(menu.getSettings(), config);
        menu.setClickDelay(config.getString("delay-message"), plugin.parseTime(config, "click-delay", 0) * 50L);
        Map<String, AbstractConfig> items = new HashMap<>();
        if (config.isSection("items")) {
            AbstractConfig cfg = config.getSection("items");
            cfg.values().forEach((key, value) -> {
                if (value instanceof AbstractConfig) {
                    items.put(key, (AbstractConfig) value);
                }
            });
        } else if (config.isList("items")) {
            List<AbstractConfig> list = config.getSectionList("items");
            for (int i = 0; i < list.size(); i++) {
                items.put(Integer.toString(i + 1), list.get(i));
            }
        }
        if (items.isEmpty() && !menu.hasEmptyItem()) {
            plugin.nag("No items specified for " + menu.getName() + "!");
            return;
        }
        items.forEach((key, value) -> loadItem(menu, key, value, true, menu.getMenuGrid()::add));
    }

    protected void loadSettings(MenuSettings settings, AbstractConfig section) {
        if (section.isSet("menu-opener")) {
            ItemStack opener = plugin.parseItemStack(section.getString("menu-opener"));
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
            settings.setOpener(opener, section.getInt("menu-opener-slot", 0) - 1);
        }
        settings.setOpenOnJoin(Flag.parseBoolean(section.getString("open-on-join")));
        if (section.isString("open-sound")) {
            SoundCommand.SoundInfo sound = new SoundCommand.SoundInfo(plugin, section.getString("open-sound"));
            settings.addListener(sound::play, false);
        }
        settings.setHiddenFromCommand(Flag.parseBoolean(section.getString("hide-from-command")));

        Object command = section.get("command");
        if (command != null) {
            AnimatedMenu menu = settings.getMenu();
            String name, usage = menu.getDefaultUsage(), description = "Opens menu " + menu.getName();
            String fallbackCommand = null;
            if (command instanceof AbstractConfig) {
                AbstractConfig sec = (AbstractConfig) command;
                name = sec.getString("name");
                usage = sec.getString("usage", usage);
                description = sec.getString("description", description);
                fallbackCommand = sec.getString("fallback");
                if (fallbackCommand != null) {
                    fallbackCommand = fallbackCommand.trim();
                } else if (!sec.getBoolean("lenient-args", true)) {
                    fallbackCommand = "";
                }
            } else if (command instanceof String || AbstractConfig.isPrimitiveWrapper(command)) {
                name = command.toString();
            } else name = null;

            if (name != null) {
                String[] split = name.split(";");
                name = split[0].trim().toLowerCase(Locale.ENGLISH);
                String[] aliases = new String[split.length - 1];
                for (int i = 0; i < aliases.length;) {
                    aliases[i] = split[++i].trim().toLowerCase(Locale.ENGLISH);
                }

                settings.setOpenCommand(name, usage, description, aliases);
                settings.setFallbackCommand(fallbackCommand);
            }
        }
    }

    public void loadItem(AbstractMenu menu, String name, AbstractConfig section,
                         boolean withSlot, Consumer<ItemInfo> action) {
        try {
            action.accept(new ConfigItemInfo(plugin, this, menu, name, section));
        } catch (IllegalArgumentException ex) {
            plugin.nag(ex.getMessage());
        }
    }

    public ClickHandler.Entry parseClickHandler(String path, AbstractConfig section) {
        try {
            return new ClickHandler.Entry(path, plugin, section);
        } catch (IllegalArgumentException ex) {
            plugin.nag(ex.getMessage() + " in " + path);
            return null;
        }
    }

    public OpenAnimation.Type resolveAnimationType(String name) {
        try {
            return OpenAnimation.DefaultType.valueOf(name);
        } catch (IllegalArgumentException ex) {
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
                        viewers.forEach(menu::open);
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
