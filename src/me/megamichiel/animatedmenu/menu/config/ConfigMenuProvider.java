package me.megamichiel.animatedmenu.menu.config;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.command.SoundCommand;
import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animatedmenu.menu.IMenuProvider;
import me.megamichiel.animatedmenu.menu.Menu;
import me.megamichiel.animatedmenu.menu.item.IMenuItem;
import me.megamichiel.animatedmenu.util.DirectoryListener;
import me.megamichiel.animatedmenu.util.DirectoryListener.FileAction;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animatedmenu.util.PluginMessage;
import me.megamichiel.animationlib.animation.AbsAnimatable;
import me.megamichiel.animationlib.animation.IAnimatable;
import me.megamichiel.animationlib.config.ConfigException;
import me.megamichiel.animationlib.config.ConfigFile;
import me.megamichiel.animationlib.config.ConfigSection;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfigMenuProvider implements IMenuProvider<Menu>, DirectoryListener.FileListener {

    protected final Map<String, Menu> menus = new ConcurrentHashMap<>();

    private final AnimatedMenuPlugin plugin;
    private final File directory;

    private DirectoryListener listener;

    public ConfigMenuProvider(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;

        File menus = directory = new File(plugin.getDataFolder(), "menus");
        if (!menus.exists()) {
            if (menus.mkdirs()) {
                saveDefaultMenus();
            } else {
                plugin.nag("Failed to create menus folder!");
            }
        }
    }

    protected void saveDefaultMenus() {
        plugin.saveResource("menus/example.yml", false);
    }

    public void onDisable() {
        if (listener != null) {
            listener.stop();
        }
        for (Menu menu : menus.values()) {
            menu.closeAll(ChatColor.RED + "Menu removed");
            menu.unregisterCommand();
        }
        menus.clear();
    }

    public int loadConfig() {
        Logger logger = plugin.getLogger();
        logger.info("Loading menus...");

        File file = new File(plugin.getDataFolder(), "images");
        if (!file.exists() && !file.mkdir()) {
            logger.warning("Failed to create images folder!");
        }

        Map<String, Menu> map = this.menus;

        loadMenus(map);
        int size = map.size();
        logger.info(size + (size == 1 ? " menu" : " menus") + " loaded");

        String plugin = this.plugin.getName();
        for (Menu menu : map.values()) {
            menu.registerCommand(plugin);
        }

        return size;
    }

    protected void loadMenus(Map<String, Menu> menus) {
        if (listener != null) {
            listener.stop();
            listener = null;
        }

        Queue<File> queue = new ArrayDeque<>();
        queue.add(directory);

        for (File file; (file = queue.poll()) != null; ) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    Collections.addAll(queue, files);
                }
                continue;
            }
            String name = file.getName();
            int index = name.lastIndexOf('.');
            if (index >= 0 && name.regionMatches(true, index, ".yml", 0, 4)) {
                YamlConfig config;
                try {
                    config = ConfigFile.quickLoad(YamlConfig::new, file);
                } catch (ConfigException ex) {
                    plugin.nag("Failed to load menu file " + file.getName() + "!");
                    plugin.nag(ex);
                    continue;
                }
                if (Flag.parseBoolean(config.getString("enable"), true)) {
                    Menu menu = loadMenu(name.substring(0, index).replace(' ', '-'), config);
                    menus.put(menu.getName().toLowerCase(Locale.ENGLISH), menu);
                }
            }
        }


        if (Flag.parseBoolean(plugin.getConfiguration().getString("auto-menu-refresh"))) {
            try {
                listener = new DirectoryListener(plugin.getLogger(), directory, this);
            } catch (IOException ex) {
                plugin.nag("Unable to set up directory update listener!");
                plugin.nag(ex);
            }
        }
    }

    protected Menu createMenu(String name, ConfigSection config, IAnimatable<? extends IPlaceholder<String>> title, AbstractMenu.Type type) {
        return new Menu(plugin, name, title, type);
    }

    protected AbstractMenu.Type getMenuType(ConfigSection config) {
        AbstractMenu.Type type;
        String typeString = config.getString("menu-type");
        if (typeString == null || "CHEST".equals(typeString = typeString.toUpperCase(Locale.ENGLISH))) {
            return AbstractMenu.Type.chest(config.getInt("rows", 6));
        }
        if ((type = AbstractMenu.Type.fromName(typeString)) == null) {
            plugin.nag("Couldn't find a menu type by name " + typeString + "!");
            return AbstractMenu.Type.chest(6);
        }
        return type;
    }

    public Menu loadMenu(String name, ConfigSection config) {
        AbsAnimatable<StringBundle> title = AbsAnimatable.ofText(true);
        if (title.load(plugin, config, "title").isEmpty() && title.load(plugin, config, "menu-name").isEmpty()) {
            title.add(new StringBundle(null, name));
        }
        title.setDelay(parseTime(config, "title-update-delay", 20));
        Menu menu = createMenu(name, config, title, getMenuType(config));

        loadSettings(menu, menu.getSettings(), config);
        loadMenu(menu, config);

        return menu;
    }

    public void loadMenu(AbstractMenu menu, ConfigSection config) {
        long delay = parseTime(config, "click-delay", 0) * 50L;
        menu.setClickDelay(delay <= 0 ? null : plugin.addPlayerDelay("menu_" + menu.getName(), config.getString("delay-message"), delay));
        Map<String, ConfigSection> items = new HashMap<>();
        if (config.isSection("items")) {
            ConfigSection cfg = config.getSection("items");
            cfg.values().forEach((key, value) -> {
                if (value instanceof ConfigSection) {
                    items.put(key, (ConfigSection) value);
                }
            });
        } else if (config.isList("items")) {
            List<ConfigSection> list = config.getSectionList("items");
            for (int i = 0; i < list.size(); i++) {
                items.put(Integer.toString(i + 1), list.get(i));
            }
        }
        if (items.isEmpty() && menu.getEmptyItem() == null) {
            plugin.nag("No items specified for " + menu.getName() + "!");
            return;
        }
        Consumer<IMenuItem> add = menu.getGrid()::add;
        items.forEach((key, value) -> loadItem(menu, key, value, true, add));
    }

    protected void loadSettings(Menu menu, Menu.Settings settings, ConfigSection config) {
        StringBundle permission = StringBundle.parse(plugin, config.getString("permission"));
        if (permission != null) {
            StringBundle message = StringBundle.parse(plugin, config.getString("permission-message"));

            IPlaceholder<String> perm = permission.tryCache(),
                    msg = message == null ? null : message.colorAmpersands().tryCache();

            menu.getSettings().addOpenFilter(player -> player.hasPermission(perm.invoke(plugin, player)) ? null : msg == null ? "" : msg.invoke(plugin, player));
        }

        Object opener = config.get("menu-opener");
        if (opener != null) {
            ItemStack item;
            String name;
            List<String> lore;
            int slot;
            if (opener instanceof ConfigSection) {
                ConfigSection section = (ConfigSection) opener;
                item = plugin.parseItemStack(section.getString("item"));
                name = section.getString("name");
                lore = section.getStringList("lore");
                slot = section.getInt("slot");
            } else {
                item = plugin.parseItemStack(config.getString("menu-opener"));
                name = config.getString("menu-opener-name");
                lore = config.getStringList("menu-opener-lore");
                slot = config.getInt("menu-opener-slot");
            }
            if (name != null || !lore.isEmpty()) {
                ItemMeta meta = item.getItemMeta();
                if (name != null) {
                    meta.setDisplayName(StringBundle.colorAmpersands(name));
                }
                if (!lore.isEmpty()) {
                    meta.setLore(lore.stream().map(StringBundle::colorAmpersands).collect(Collectors.toList()));
                }
                item.setItemMeta(meta);
            }
            settings.setOpener(item, slot - 1);
        }
        settings.setOpenOnJoin(Flag.parseBoolean(config.getString("open-on-join")));
        if (config.isString("open-sound")) {
            SoundCommand.SoundInfo sound = new SoundCommand.SoundInfo(plugin, config.getString("open-sound"));
            settings.addListener(sound::play, false);
        }
        settings.setHiddenFromCommand(Flag.parseBoolean(config.getString("hide-from-command")));

        Object command = config.get("command");
        if (command != null) {
            String name, usage = menu.getDefaultUsage(), description = "Opens menu " + menu.getName();
            String fallbackCommand = null;
            if (command instanceof ConfigSection) {
                ConfigSection section = (ConfigSection) command;
                name = section.getString("name");
                usage = section.getString("usage", usage);
                description = section.getString("description", description);
                if ((fallbackCommand = section.getString("fallback")) == null && !section.getBoolean("lenient-args", true)) {
                    fallbackCommand = "";
                }
            } else {
                name = command instanceof String || ConfigSection.isPrimitiveWrapper(command) ? command.toString() : null;
            }

            if (name != null) {
                String[] split = name.split(";"), aliases = new String[split.length - 1];
                for (int i = 0; i < aliases.length; ) {
                    aliases[i] = split[++i].trim().toLowerCase(Locale.ENGLISH);
                }

                settings.setOpenCommand(menu, split[0].trim().toLowerCase(Locale.ENGLISH), usage, description, aliases);

                String[] fallback;
                if (fallbackCommand == null || (fallback = fallbackCommand.split(";")).length == 0 || (fallback.length == 1 && fallback[0].isEmpty())) {
                    settings.setFallbackExecutor(ctx -> ((CommandSender) ctx.getSender()).sendMessage(plugin.format(ctx.getSender() instanceof Player ? PluginMessage.COMMAND__TOO_MANY_ARGUMENTS : PluginMessage.COMMAND__NOT_PLAYER)));
                } else {
                    Map<String, String> map = new HashMap<>();

                    String defCommand = fallback[0].trim().toLowerCase(Locale.ENGLISH);
                    for (int i = 0, len = Math.min(aliases.length, fallback.length - 1); i < len; ) {
                        map.put(aliases[i], fallback[++i].trim().toLowerCase(Locale.ENGLISH));
                    }
                    settings.setFallbackExecutor(ctx -> {
                        StringBuilder commandLine = new StringBuilder(map.getOrDefault(ctx.getLabel().toLowerCase(Locale.ENGLISH), defCommand));
                        for (String arg : ctx.getArgs()) {
                            commandLine.append(' ').append(arg);
                        }
                        plugin.getServer().dispatchCommand((CommandSender) ctx.getSender(), commandLine.toString());
                    });
                }
            }
        }
    }

    public void loadItem(AbstractMenu menu, String name, ConfigSection section,
                         boolean withSlot, Consumer<IMenuItem> action) {
        try {
            action.accept(new ConfigMenuItem(plugin, this, menu, name, section));
        } catch (IllegalArgumentException ex) {
            plugin.nag(ex.getMessage());
        }
    }

    public short resolveDataValue(Material type, String data) {
        try {
            return (short) Integer.parseInt(data);
        } catch (NumberFormatException ex) {
            plugin.nag("Invalid data value: " + data);
            return 0;
        }
    }

    public int parseTime(ConfigSection config, String key, int def) {
        Object o = config.get(key, def);
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        plugin.nag("Invalid " + key + ": " + o);
        return 0;
    }

    protected ClickHandler.Entry parseClickHandler(String path, ConfigSection section) {
        try {
            return new ClickHandler.Entry(path, plugin, this, section);
        } catch (IllegalArgumentException ex) {
            plugin.nag(ex.getMessage() + " in " + path);
            return null;
        }
    }

    @Override
    public void fileChanged(File file, FileAction action) {
        if (file.isFile() || action == FileAction.DELETE) {
            String fileName = file.getName();
            int index = fileName.lastIndexOf('.');
            if (index >= 0 && file.toPath().startsWith(directory.toPath()) && fileName.regionMatches(index, ".yml", 0, 4)) {
                String name = fileName.substring(0, index).replace(" ", "_"), lower = name.toLowerCase(Locale.ENGLISH);
                switch (action) {
                    case CREATE:
                        Menu menu = menus.remove(lower);
                        if (menu != null) {
                            plugin.getLogger().warning("A new menu file was created, but a menu already existed with its name! Beware!");

                            menu.closeAll(ChatColor.RED + "Menu reloaded");
                            menu.unregisterCommand();
                        }
                        YamlConfig config;
                        try {
                            config = ConfigFile.quickLoad(YamlConfig::new, file);
                        } catch (ConfigException ex) {
                            plugin.nag("Failed to load menu file " + fileName + "!");
                            plugin.nag(ex);
                            return;
                        }
                        if (Flag.parseBoolean(config.getString("enable"), true)) {
                            menus.put(lower, menu = loadMenu(name, config));
                            menu.registerCommand(plugin.getName());

                            plugin.getLogger().info("Loaded " + fileName);
                        }

                        break;
                    case MODIFY:
                        if (file.length() == 0) {
                            return;
                        }

                        if ((menu = menus.remove(lower)) != null) {
                            for (Player viewer : menu.getViewers()) {
                                viewer.sendMessage(ChatColor.RED + "Menu reloaded");
                                viewer.closeInventory();
                            }
                            menu.unregisterCommand();
                        }

                        try {
                            config = ConfigFile.quickLoad(YamlConfig::new, file);
                        } catch (ConfigException ex) {
                            plugin.nag("Failed to load menu file " + fileName + "!");
                            plugin.nag(ex);
                            return;
                        }
                        if (Flag.parseBoolean(config.getString("enable"), true)) {
                            menus.put(lower, menu = loadMenu(name, config));
                            menu.registerCommand(plugin.getName());
                            
                            plugin.getLogger().info("Updated " + fileName);
                        }
                        break;
                    case DELETE:
                        if ((menu = menus.remove(lower)) != null) {
                            menu.closeAll(ChatColor.RED + "Menu removed");
                            menu.unregisterCommand();

                            plugin.getLogger().info("Removed " + fileName);
                        }
                        break;
                }
            }
        }
    }

    @Override
    public Iterator<Menu> iterator() {
        return menus.values().iterator();
    }

    @Override
    public void forEach(Consumer<? super Menu> action) {
        menus.values().forEach(action);
    }

    @Override
    public Menu getMenu(String name) {
        return menus.get(name);
    }
}
