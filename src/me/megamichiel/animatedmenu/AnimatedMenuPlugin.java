package me.megamichiel.animatedmenu;

import me.jacobculley.actionapi.ActionAPI;
import me.megamichiel.animatedmenu.command.*;
import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animatedmenu.menu.Menu;
import me.megamichiel.animatedmenu.menu.MenuRegistry;
import me.megamichiel.animatedmenu.menu.MenuSession;
import me.megamichiel.animatedmenu.menu.config.ConfigMenuProvider;
import me.megamichiel.animatedmenu.util.*;
import me.megamichiel.animatedmenu.util.item.MaterialParser;
import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.animation.IAnimatable;
import me.megamichiel.animationlib.bukkit.nbt.NBTUtil;
import me.megamichiel.animationlib.config.ConfigFile;
import me.megamichiel.animationlib.config.ConfigSection;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.CtxPlaceholder;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
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
import org.bukkit.inventory.*;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.bukkit.ChatColor.*;

public class AnimatedMenuPlugin extends JavaPlugin implements Listener, LoggerNagger, PipelineContext {

    private static final String ANIMLIB_VERSION = "1.6.4";

    protected final Map<String, Command<?, ?>> commands = new HashMap<>();
    protected final AnimatedMenuCommand command = new AnimatedMenuCommand(this);

    private final MenuRegistry menuRegistry = new MenuRegistry(this);
    private ConfigMenuProvider configMenuProvider;
    private BukkitTask menuTask;

    private final Set<Delay> delays = Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<String, Map<UUID, Long>> loadedDelays = new HashMap<>();
    private final ConfigFile<YamlConfig> config = ConfigFile.of(YamlConfig::new);

    protected final RemoteConnections connections = new RemoteConnections(this);

    protected final Map<PluginMessage, PluginMessage.FormatText> parsedMessages = new ConcurrentHashMap<>();
    private String messagePrefix;

    private boolean detailedErrors = false;
    private String update;

    public AnimatedMenuPlugin() {
        Bukkit.getServicesManager().register(MenuRegistry.class, menuRegistry, this, ServicePriority.Normal);
    }

    protected void init(ConfigMenuProvider configMenuProvider) {
        this.configMenuProvider = configMenuProvider;
    }
    
    @Override
    public void onEnable() {
        menuRegistry.registerProvider(configMenuProvider == null ? (configMenuProvider = new ConfigMenuProvider(this)) : configMenuProvider);

        Plugin plugin = getServer().getPluginManager().getPlugin("AnimationLib");
        boolean disable = true;
        if (plugin != null) {
            String str = plugin.getDescription().getVersion();
            if (str.equals(ANIMLIB_VERSION) || Double.parseDouble("0." + str.replace(".", ""))
                    >= Double.parseDouble("0." + ANIMLIB_VERSION.replace(".", ""))) {
                try {
                    Class.forName("me.megamichiel.animationlib.AnimLib");
                    disable = false;
                } catch (ClassNotFoundException ex) {
                    // Wrong AnimationLib
                }
            }
        }
        if (disable) {
            getServer().getConsoleSender().sendMessage(new String[] {
                    RED + "[AnimatedMenu] I require AnimationLib v" + ANIMLIB_VERSION + " to work!",
                    RED + "Download it at \"https://www.spigotmc.org/resources/22295/\""
            });
            getServer().getPluginManager().disablePlugin(this);
            getServer().getServicesManager().unregister(MenuRegistry.class, menuRegistry);
            return;
        }

        /* Listeners */
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getCommand("animatedmenu").setExecutor(command);
        
        /* Config / API */
        config.file(new File(getDataFolder(), "config.yml")).saveDefaultConfig(() -> getResource("config.yml"));

        AnimatedMenuPlaceholders.register(this, connections);
        loadDelays();
        loadConfig(false, false);
        post(configMenuProvider::loadConfig, SYNC);

        /* Other Stuff */
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            String current = getDescription().getVersion();
            try {
                String version = AnimLib.getVersion(4690);
                if ((update = current.equals(version) ? null : version) != null) {
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

        menuTask = Flag.parseBoolean(config.getConfig().getString("run-sync"), true) ? getServer().getScheduler().runTaskTimer(this, menuRegistry, 0, 0) : getServer().getScheduler().runTaskTimerAsynchronously(this, menuRegistry, 0, 0);
    }

    @Override
    public void onDisable() {
        if (getServer().getServicesManager().getRegistration(MenuRegistry.class) == null) {
            return;
        }
        getServer().getServicesManager().unregister(MenuRegistry.class, menuRegistry);

        if (menuTask != null) {
            menuTask.cancel();
            menuTask = null;
        }
        connections.cancel();
        saveDelays();
        configMenuProvider.onDisable();

        PluginManager pm = getServer().getPluginManager();
        for (PluginPermission perm : PluginPermission.values()) {
            perm.unregister(pm);
        }
    }

    public YamlConfig getConfiguration() {
        return config.getConfig();
    }
    
    protected void loadConfig(boolean reload, boolean premium) {
        messagePrefix = formatRaw(PluginMessage.PREFIX);

        ConfigSection config = getConfiguration();
        if (config.isSection("connections")) {
            ConfigSection section = config.getSection("connections");
            section.forEach((key, value) -> {
                if (!(value instanceof ConfigSection)) {
                    return;
                }
                ConfigSection sec = (ConfigSection) value;
                String ip = sec.getString("ip");
                if (ip != null) {
                    int index = ip.indexOf(':');
                    connections.add(section.getOriginalKey(key), new InetSocketAddress(
                            index == -1 ? ip : ip.substring(0, index),
                            index == -1 ? 25565 : Integer.parseInt(ip.substring(index + 1))
                    ), sec);
                }
            });
            connections.schedule(config.getLong("connection-refresh-delay", 10 * 20L));
        }
        detailedErrors = Flag.parseBoolean(config.getString("detailed-errors"));

        if (!reload) {
            PluginManager pm = getServer().getPluginManager();
            for (PluginPermission perm : PluginPermission.values()) {
                if (premium || !perm.isPremium()) {
                    try {
                        perm.register(pm);
                    } catch (IllegalArgumentException ex) {
                        nag("Failed to register permission " + perm + " because it already exists");
                    }
                }
            }

            Stream.of(
                    TextCommand.ofPlayer("chat", false, Player::chat),
                    TextCommand.of("console", false, value -> getServer().dispatchCommand(getServer().getConsoleSender(), value)),
                    TextCommand.ofPlayer("message", true, Player::sendMessage),
                    TextCommand.ofPlayer("op", false, (p, value) -> {
                        if (p.isOp()) {
                            p.performCommand(value);
                        } else {
                            p.setOp(true);
                            p.performCommand(value);
                            p.setOp(false);
                        }
                    }),
                    TextCommand.of("broadcast", true, getServer()::broadcastMessage),
                    TextCommand.ofPlayer("menu", false, (p, value) -> {
                        AbstractMenu menu = menuRegistry.getMenu(value);
                        if (menu instanceof Menu) {
                            ((Menu) menu).open(p);
                        } else {
                            String text = format(PluginMessage.MENU__NOT_FOUND, "menu", value);
                            p.sendMessage(text);
                            getServer().getConsoleSender().sendMessage(text);
                        }
                    }),
                    new ServerCommand(),
                    new SoundCommand(),
                    new Command<StringBundle, ItemStack>("give") {

                        private ItemStack _parse(String value) {
                            int index = value.indexOf(' ');
                            if (index == -1) {
                                return parseItemStack(value);
                            }
                            NBTUtil util = NBTUtil.getInstance();
                            ItemStack item = parseItemStack(value.substring(0, index));
                            if (item != (item = util.asNMS(item))) {
                                util.setTag(item, util.parse(value.substring(index + 1).trim()));
                            }
                            return item;
                        }

                        @Override
                        protected StringBundle parse(Nagger nagger, String command) {
                            return StringBundle.parse(nagger, command).colorAmpersands();
                        }

                        @Override
                        protected boolean execute(AnimatedMenuPlugin plugin, Player p, StringBundle value) {
                            return executeCached(plugin, p, _parse(value.toString(p)));
                        }

                        @Override
                        protected ItemStack tryCacheValue(AnimatedMenuPlugin plugin, StringBundle value) {
                            return value.containsPlaceholders() ? null : _parse(value.toString(null));
                        }

                        @Override
                        protected boolean executeCached(AnimatedMenuPlugin plugin, Player p, ItemStack value) {
                            p.getInventory().addItem(value);
                            return true;
                        }
                    },
                    new TextCommand("action", false) {
                        Plugin api;

                        @Override
                        protected boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
                            if (api == null || !api.isEnabled()) {
                                try {
                                    api = ActionAPI.getInstance();
                                } catch (NoClassDefFoundError err) {
                                    plugin.nag("Couldn't find ActionAPI plugin!");
                                    return true;
                                }
                            }
                            ((ActionAPI) api).getAPI().executeAction(p, value);
                            return true;
                        }
                    }
            ).forEach(this::registerCommand);

            try {
                registerCommand(new TellRawCommand());
            } catch (IllegalStateException ex) {
                nag("Unable to load chat message class! Tellraw won't be usable!");
                nag(ex.getCause());
            }
        }
    }
    
    int reload() {
        saveDelays();

        configMenuProvider.onDisable();
        config.reloadConfig();

        connections.cancel();
        parsedMessages.clear();
        loadConfig(true, false);

        return configMenuProvider.loadConfig();
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
        if (!file.isFile()) {
            return;
        }
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
        if (isOffHand.test(e)) {
            return;
        }
        ItemStack item = e.getItem();
        if (PluginPermission.OPEN_WITH_ITEM.test(p) && item != null) {
            ItemMeta meta = item.getItemMeta();
            for (AbstractMenu menu : menuRegistry) {
                if (menu instanceof Menu && ((Menu) menu).getSettings().canOpenWith(item, meta)) {
                    e.setCancelled(true);
                    post(() -> ((Menu) menu).open(p), SYNC);
                    return;
                }
            }
        }
    }
    
    @EventHandler
    public void on(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) {
            return;
        }
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof MenuSession) {
            AbstractMenu menu = ((MenuSession) holder).getMenu();
            if (menu instanceof Menu) {
                ((Menu) menu).handleMenuClose((Player) e.getPlayer(), null);
            }
        }
    }
    
    @EventHandler
    public void on(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory inventory = e.getView().getTopInventory();
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof MenuSession) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                ((MenuSession) holder).click(e.getSlot(), e.getClick());
            }
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

    public final String format(PluginMessage message, Object... args) {
        return messagePrefix + formatRaw(message, args);
    }

    public String formatRaw(PluginMessage message, Object... args) {
        return parsedMessages.computeIfAbsent(message, $ -> new PluginMessage.FormatText(message.getDefault(), args)).format(args);
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
        try {
            amount = (amount = Integer.parseInt(split[1])) < 0 ? 0 : amount > 127 ? 127 : amount;
        } catch (NumberFormatException ex) {
            nag("Invalid amount in " + str + "! Defaulting to 1");
        }
        return new ItemStack(type, amount, split.length == 2 ? 0 : configMenuProvider.resolveDataValue(type, split[2]));
    }

    public IPlaceholder<ItemStack> parseItemPlaceholder(Nagger nagger, Object o) {
        StringBundle sb = StringBundle.parse(nagger, o.toString());
        if (sb.containsPlaceholders()) {
            if (sb.isAnimated()) {
                class ItemPlaceholder implements CtxPlaceholder<ItemStack>, IAnimatable<ItemPlaceholder> {

                    @Override
                    public ItemPlaceholder get() {
                        return this;
                    }

                    @Override
                    public boolean isAnimated() {
                        return true;
                    }

                    @Override
                    public boolean tick() {
                        return sb.tick();
                    }

                    @Override
                    public ItemStack invoke(Nagger nagger, Object o, PlaceholderContext ctx) {
                        return parseItemStack(sb.invoke(nagger, o, ctx));
                    }
                }

                return new ItemPlaceholder();
            }

            return (CtxPlaceholder<ItemStack>) (n, p, ctx) -> parseItemStack(sb.invoke(n, p, ctx));
        }
        return IPlaceholder.constant(parseItemStack(sb.toString(null)));
    }

    public Command<?, ?> findCommand(String name) {
        return commands.getOrDefault(name, TextCommand.DEFAULT);
    }

    public boolean registerCommand(Command<?, ?> command) {
        return commands.putIfAbsent(command.getPrefix(), command) == null;
    }

    public boolean unregisterCommand(Command<?, ?> command) {
        return commands.remove(command.getPrefix(), command);
    }

    public MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    @Override
    public void onClose() {}

    @Override
    public void post(Runnable task, boolean async) {
        if (async) {
            getServer().getScheduler().runTaskAsynchronously(this, task);
        } else {
            getServer().getScheduler().runTask(this, task);
        }
    }

    @Override
    public void nag(Throwable throwable) {
        if (detailedErrors) {
            throwable.printStackTrace();
        } else {
            getLogger().warning(throwable.toString());
        }
    }
}
