package me.megamichiel.animatedmenu;

import me.megamichiel.animatedmenu.command.*;
import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animatedmenu.menu.Menu;
import me.megamichiel.animatedmenu.menu.MenuRegistry;
import me.megamichiel.animatedmenu.menu.MenuSession;
import me.megamichiel.animatedmenu.menu.config.MenuLoader;
import me.megamichiel.animatedmenu.util.Delay;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animatedmenu.util.PluginPermission;
import me.megamichiel.animatedmenu.util.RemoteConnections;
import me.megamichiel.animatedmenu.util.item.MaterialParser;
import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bukkit.nbt.NBTUtil;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.LoggerNagger;
import me.megamichiel.animationlib.util.ReflectClass;
import me.megamichiel.animationlib.util.pipeline.PipelineContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.bukkit.ChatColor.*;

public class AnimatedMenuPlugin extends JavaPlugin implements Listener, LoggerNagger, PipelineContext {

    private static AnimatedMenuPlugin instance;

    public static AnimatedMenuPlugin get() {
        return instance;
    }

    protected final Map<String, Command<?, ?>> commands = new HashMap<>();
    protected final AnimatedMenuCommand command = new AnimatedMenuCommand(this);
    protected final MenuLoader menuLoader;

    private final MenuRegistry menuRegistry;
    private final Set<Delay> delays = Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<String, Map<UUID, Long>> loadedDelays = new HashMap<>();
    private final List<BukkitTask> asyncTasks = new ArrayList<>();
    private final ConfigManager<YamlConfig> config = ConfigManager.of(YamlConfig::new);
    private final RemoteConnections connections = new RemoteConnections(this);

    private boolean warnOfflineServers = false;
    private String update;
    private boolean runningSync;

    public AnimatedMenuPlugin() {
        this(MenuRegistry::new, MenuLoader::new);
    }

    protected AnimatedMenuPlugin(Function<AnimatedMenuPlugin, MenuRegistry> registry, Function<AnimatedMenuPlugin, MenuLoader> loader) {
        instance = this;

        menuRegistry = registry.apply(this);
        menuLoader = loader.apply(this);

        Bukkit.getServicesManager().register(MenuRegistry.class, menuRegistry, this, ServicePriority.Normal);
    }
    
    @Override
    public void onEnable() {
        if (!checkAnimationLib()) return;
        else {
            try {
                Class.forName("me.megamichiel.animationlib.AnimLib");
            } catch (ClassNotFoundException ex) {
                getServer().getConsoleSender().sendMessage(new String[]{
                        RED + "It appears you have the wrong AnimationLib plugin!",
                        RED + "Download the correct one at https://www.spigotmc.org/resources/22295/"
                });
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        registerPermissions();

        /* Listeners */
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getCommand("animatedmenu").setExecutor(command);
        
        /* Config / API */
        config.file(new File(getDataFolder(), "config.yml"));
        saveDefaultConfig();
        
        registerDefaultCommandHandlers();
        AnimatedMenuPlaceholders.register(this);
        loadDelays();
        post(menuLoader::loadConfig, SYNC);
        
        /* Other Stuff */
        checkForUpdate();
        loadConfig();

        if (runningSync = Flag.parseBoolean(getConfiguration().getString("run-sync"))) {
            getServer().getScheduler().runTaskTimer(this, menuRegistry, 0, 0);
        } else asyncTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, menuRegistry, 0, 0));
    }

    private static final String ANIMLIB_VERSION = "1.6.2";

    private boolean checkAnimationLib() {
        Plugin plugin = getServer().getPluginManager().getPlugin("AnimationLib");
        if (plugin != null) {
            String str = plugin.getDescription().getVersion();
            if (str.equals(ANIMLIB_VERSION) || Double.parseDouble("0." + str.replace(".", ""))
                    >= Double.parseDouble("0." + ANIMLIB_VERSION.replace(".", ""))) {
                try {
                    Class.forName("me.megamichiel.animationlib.AnimLib");
                    return true;
                } catch (ClassNotFoundException ex) {
                    // Wrong AnimationLib
                }
            }
        }
        getServer().getConsoleSender().sendMessage(new String[] {
                RED + "[AnimatedMenu] I require AnimationLib v" + ANIMLIB_VERSION + " to work!",
                RED + "Download it at \"https://www.spigotmc.org/resources/22295/\""
        });
        getServer().getPluginManager().disablePlugin(this);
        return false;
    }

    @Override
    public void onDisable() {
        asyncTasks.forEach(BukkitTask::cancel);
        asyncTasks.clear();
        connections.cancel();
        saveDelays();
        menuLoader.onDisable();
        menuRegistry.onDisable();

        PluginManager pm = getServer().getPluginManager();
        for (PluginPermission perm : PluginPermission.values()) {
            perm.unregister(pm);
        }
    }

    @Override
    public void reloadConfig() {
        config.reloadConfig();
    }

    public YamlConfig getConfiguration() {
        return config.getConfig();
    }

    @Override
    public void saveDefaultConfig() {
        config.saveDefaultConfig(() -> getResource("config.yml"));
    }

    protected void registerPermissions() {
        PluginManager pm = getServer().getPluginManager();
        for (PluginPermission perm : PluginPermission.values()) {
            if (!perm.isPremium()) {
                try {
                    perm.register(pm);
                } catch (IllegalArgumentException e) {
                    nag("Failed to register permission " + perm + " because it already exists");
                }
            }
        }
    }
    
    private void checkForUpdate() {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            String current = getDescription().getVersion();
            try {
                String version = AnimLib.getVersion(4690);
                update = current.equals(version) ? null : version;
                if (update != null) {
                    getLogger().info("A new version is available! (Current version: " + current + ", new version: " + update + ")");
                    update = String.format(
                            "%s[%s%s%1$s] %sA new version is available! (Current version: %s, new version: %s)",
                            DARK_GRAY, GOLD, getDescription().getName(), GREEN, getDescription().getVersion(), update);
                }
            } catch (IOException ex) {
                nag("Failed to check for updates:");
                nag(ex);
            }
        });
    }
    
    protected void registerDefaultCommandHandlers() {
        commands.clear();
        Stream.of(
                TextCommand.ofPlayer("chat", false, Player::chat),
                TextCommand.of("console", false, value -> getServer().dispatchCommand(getServer().getConsoleSender(), value)),
                TextCommand.ofPlayer("message", true, Player::sendMessage),
                TextCommand.ofPlayer("op", false, (p, value) -> {
                    if (p.isOp()) p.performCommand(value);
                    else {
                        p.setOp(true);
                        p.performCommand(value);
                        p.setOp(false);
                    }
                }),
                TextCommand.of("broadcast", true, getServer()::broadcastMessage),
                TextCommand.ofPlayer("menu", false, (p, value) -> {
                    MenuRegistry registry = getMenuRegistry();
                    AbstractMenu menu = registry.getMenu(value);
                    if (menu instanceof Menu) {
                        ((Menu) menu).open(p);
                    } else {
                        nag("No menu with name " + value + " found!");
                    }
                }),
                new Command<StringBundle, ItemStack>("give") {

                    private ItemStack _parse(AnimatedMenuPlugin plugin, String value) {
                        int index = value.indexOf(' ');
                        if (index == -1) {
                            return plugin.parseItemStack(value);
                        } else {
                            NBTUtil util = NBTUtil.getInstance();
                            ItemStack item = parseItemStack(value.substring(0, index));
                            if (item != (item = util.asNMS(item))) {
                                util.setTag(item, util.parse(value.substring(index + 1).trim()));
                            }
                            return item;
                        }
                    }

                    @Override
                    protected StringBundle parse(Nagger nagger, String command) {
                        return StringBundle.parse(nagger, command).colorAmpersands();
                    }

                    @Override
                    protected boolean execute(AnimatedMenuPlugin plugin, Player p, StringBundle value) {
                        return executeCached(plugin, p, _parse(plugin, value.toString(p)));
                    }

                    @Override
                    protected ItemStack tryCacheValue(AnimatedMenuPlugin plugin, StringBundle value) {
                        return value.containsPlaceholders() ? null : _parse(plugin, value.toString(null));
                    }

                    @Override
                    protected boolean executeCached(AnimatedMenuPlugin plugin, Player p, ItemStack value) {
                        p.getInventory().addItem(value);
                        return true;
                    }
                }, new ServerCommand(), new TellRawCommand(this), new SoundCommand()
        ).forEach(this::registerCommand);
    }
    
    public boolean warnOfflineServers() {
        return warnOfflineServers;
    }
    
    protected void loadConfig() {
        AbstractConfig config = getConfiguration();
        if (config.isSection("connections")) {
            AbstractConfig section = config.getSection("connections");
            section.forEach((key, value) -> {
                if (!(value instanceof AbstractConfig)) return;
                AbstractConfig sec = (AbstractConfig) value;
                String ip = sec.getString("ip");
                if (ip != null) {
                    int index = ip.indexOf(':');
                    int port;
                    if (index == -1) {
                        port = 25565;
                    } else {
                        port = Integer.parseInt(ip.substring(index + 1));
                        ip = ip.substring(0, index);
                    }
                    connections.add(section.getOriginalKey(key), new InetSocketAddress(ip, port)).load(sec);
                }
            });
            warnOfflineServers = Flag.parseBoolean(config.getString("warn-offline-servers"));
            connections.schedule(config.getLong("connection-refresh-delay", 10 * 20L));
        }
    }
    
    void reload() {
        for (Delay delay : delays) {
            Map<UUID, Long> map = delay.save();
            if (map.isEmpty()) {
                loadedDelays.remove(delay.getId());
            } else {
                loadedDelays.put(delay.getId(), map);
            }
        }

        menuLoader.onDisable();
        saveDefaultConfig();
        reloadConfig();

        connections.cancel();
        loadConfig();
        
        registerDefaultCommandHandlers();
        menuLoader.loadConfig();
    }

    private void saveDelays() {
        delays.forEach(delay -> {
            Map<UUID, Long> map = delay.save();
            if (map.isEmpty()) {
                loadedDelays.remove(delay.getId());
            } else {
                loadedDelays.put(delay.getId(), map);
            }
        });
        try (FileOutputStream fos = new FileOutputStream(new File(getDataFolder(), "delays.dat"));
             DataOutputStream dos = new DataOutputStream(fos)) {

            for (Map.Entry<String, Map<UUID, Long>> entry : loadedDelays.entrySet()) {
                Map<UUID, Long> map = entry.getValue();
                dos.writeInt(map.size());
                byte[] bytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
                dos.writeInt(bytes.length);
                dos.write(bytes);

                for (Map.Entry<UUID, Long> mapEntry : map.entrySet()) {
                    UUID id = mapEntry.getKey();
                    dos.writeLong(id.getMostSignificantBits());
                    dos.writeLong(id.getLeastSignificantBits());
                    dos.writeLong(mapEntry.getValue());
                }
            }

            dos.writeInt(0);
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Failed to save delays", ex);
        }
    }

    private void loadDelays() {
        File file = new File(getDataFolder(), "delays.dat");
        if (!file.isFile()) return;
        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {
            int size;
            while ((size = dis.readInt()) > 0) {
                byte[] b = new byte[dis.readInt()];
                dis.readFully(b);
                String id = new String(b, StandardCharsets.UTF_8);
                Map<UUID, Long> map = new HashMap<>();
                while (--size >= 0) {
                    map.put(new UUID(dis.readLong(), dis.readLong()), dis.readLong());
                }
                loadedDelays.put(id, map);
            }
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Failed to load delays", ex);
        }
    }
    
    /* Listeners */
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (update != null && PluginPermission.SEE_UPDATE.test(player)) {
            player.sendMessage(update);
        }
        PlayerInventory inv = player.getInventory();
        for (AbstractMenu absMenu : menuRegistry) {
            if (absMenu instanceof Menu) {
                Menu menu = (Menu) absMenu;
                menu.getSettings().giveOpener(inv, false);
                if (menu.getSettings().shouldOpenOnJoin()) {
                    post(() -> menu.open(player), SYNC);
                }
            }
        }
    }

    private final Predicate<PlayerInteractEvent> isOffHand;

    {
        Predicate<PlayerInteractEvent> predicate;
        try {
            new ReflectClass(PlayerInteractEvent.class).getMethod("getHand");
            predicate = event -> event.getHand() == EquipmentSlot.OFF_HAND;
        } catch (ReflectClass.ReflectException ex) {
            predicate = event -> false;
        }
        isOffHand = predicate;
    }
    
    @EventHandler
    public void on(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (isOffHand.test(e)) return;
        ItemStack item = e.getItem();
        if (PluginPermission.OPEN_WITH_ITEM.test(p) && item != null) {
            ItemMeta meta = item.getItemMeta();
            for (AbstractMenu menu : menuRegistry) {
                if (menu instanceof Menu && ((Menu) menu).getSettings().canOpenWith(item, meta)) {
                    e.setCancelled(true);
                    ((Menu) menu).open(p);
                    return;
                }
            }
        }
    }
    
    @EventHandler//(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) {
            return;
        }
        menuRegistry.handleMenuClose((Player) e.getPlayer());
    }
    
    @EventHandler
    public void on(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        MenuSession session = menuRegistry.getSession(p);
        if (session == null) {
            return;
        }
        e.setCancelled(true);
        int slot = e.getRawSlot();
        InventoryView view = e.getView();
        if (view.getTopInventory() != null && slot >= 0 && slot < view.getTopInventory().getSize()) {
            session.click(e.getSlot(), e.getClick());
        }
    }

    public Delay addPlayerDelay(String id, String delayMessage, long time) {
        Delay delay = new Delay(this, id, delayMessage, time);
        Map<UUID, Long> map = loadedDelays.remove(id);
        if (map != null) {
            delay.load(map);
        }
        delays.add(delay);
        return delay;
    }

    public int parseTime(AbstractConfig config, String key, int def) {
        return parseTime(this, config.get(key, def));
    }

    public int parseTime(Nagger nagger, Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        nagger.nag("Invalid number: " + o);
        return 0;
    }

    public short resolveDataValue(Nagger nagger, Material type, String str) {
        try {
            return (short) Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            nagger.nag("Invalid data value: " + str);
            return 0;
        }
    }

    public ItemStack parseItemStack(String str) {
        String[] split = str.split(":");
        Material type = MaterialParser.parse(split[0]);
        if (type == null) {
            nag("Couldn't find appropiate material for " + split[0] + "! Defaulting to stone");
            type = Material.STONE;
        }
        if (split.length == 1) {
            return new ItemStack(type);
        }
        int amount = 1;
        short data = 0;
        try {
            amount = Integer.parseInt(split[1]);
            if (amount < 0) amount = 0;
            if (amount > 64) amount = 64;
        } catch (NumberFormatException ex) {
            nag("Invalid amount in " + str + "! Defaulting to 1");
        }
        if (split.length > 2) {
            data = resolveDataValue(this, type, split[2]);
        }
        return new ItemStack(type, amount, data);
    }

    public IPlaceholder<ItemStack> parseItemPlaceholder(Nagger nagger, String str) {
        StringBundle sb = StringBundle.parse(nagger, str);
        if (sb.containsPlaceholders()) {
            return (n, player) -> parseItemStack(sb.toString(player));
        } else {
            return IPlaceholder.constant(parseItemStack(sb.toString(null)));
        }
    }

    public Command<?, ?> findCommand(String name) {
        return commands.getOrDefault(name, TextCommand.DEFAULT);
    }

    public void registerCommand(Command<?, ?> command) {
        commands.put(command.getPrefix(), command);
    }

    public MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    RemoteConnections getConnections() {
        return connections;
    }

    @Override
    public void onClose() {}

    @Override
    public void post(Runnable task, boolean async) {
        if (async) getServer().getScheduler().runTaskAsynchronously(this, task);
        else getServer().getScheduler().runTask(this, task);
    }

    public boolean isRunningSync() {
        return runningSync;
    }
}
