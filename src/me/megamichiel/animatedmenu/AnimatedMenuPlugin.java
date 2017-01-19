package me.megamichiel.animatedmenu;

import me.megamichiel.animatedmenu.animation.OpenAnimation;
import me.megamichiel.animatedmenu.command.Command;
import me.megamichiel.animatedmenu.command.SoundCommand;
import me.megamichiel.animatedmenu.command.TellRawCommand;
import me.megamichiel.animatedmenu.command.TextCommand;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.menu.MenuLoader;
import me.megamichiel.animatedmenu.util.Delay;
import me.megamichiel.animatedmenu.util.RemoteConnections;
import me.megamichiel.animationlib.bukkit.PapiPlaceholder;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.util.LoggerNagger;
import me.megamichiel.animationlib.util.pipeline.PipelineContext;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Locale.ENGLISH;

public class AnimatedMenuPlugin extends JavaPlugin implements Listener, LoggerNagger, PipelineContext {

    protected final List<Command<?, ?>> commands = new ArrayList<>();

    private final AnimatedMenuCommand command = new AnimatedMenuCommand(this);
    private final MenuRegistry menuRegistry = new MenuRegistry(this);
    private final Set<Delay<Player>> delays = Collections.newSetFromMap(new WeakHashMap<>());

    private final RemoteConnections connections = new RemoteConnections(this);
    private boolean warnOfflineServers = true;
    
    private final List<BukkitTask> asyncTasks = new ArrayList<>();
    
    private String update;
    private boolean vaultPresent = false, playerPointsPresent = false;
    public Economy economy;
    public PlayerPointsAPI playerPointsAPI;

    private final ConfigManager<YamlConfig> config = ConfigManager.of(YamlConfig::new);
    
    @Override
    public void onEnable() {
        if (!checkAnimationLib()) return;
        else try {
            Class.forName("me.megamichiel.animationlib.AnimLib");
        } catch (ClassNotFoundException ex) {
            getServer().getConsoleSender().sendMessage(new String[]{
                    ChatColor.RED + "It appears you have the wrong AnimationLib plugin!",
                    ChatColor.RED + "Download the correct one at https://www.spigotmc.org/resources/22295/"
            });
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        /* Listeners */
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getCommand("animatedmenu").setExecutor(command);
        
        /* Config / API */
        config.file(new File(getDataFolder(), "config.yml"));
        saveDefaultConfig();
        
        registerDefaultCommandHandlers();
        if (PapiPlaceholder.apiAvailable)
            AnimatedMenuPlaceholders.register(this);
        try {
            RegisteredServiceProvider<Economy> reg = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (reg != null) {
                economy = reg.getProvider();
                vaultPresent = true;
            }
        } catch (NoClassDefFoundError err) {
            // No Vault
        }
        Plugin pp = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (pp != null && pp instanceof PlayerPoints) {
            playerPointsAPI = ((PlayerPoints) pp).getAPI();
            playerPointsPresent = true;
        }
        post(menuRegistry::loadMenus, false);
        
        /* Other Stuff */
        checkForUpdate();
        loadConfig();

        if (getConfiguration().getBoolean("run-sync"))
            getServer().getScheduler().runTaskTimer(this, menuRegistry, 0, 0);
        else asyncTasks.add(getServer().getScheduler().runTaskTimerAsynchronously(this, menuRegistry, 0, 0));
    }

    private static final String ANIMLIB_VERSION = "1.5.5";

    private boolean checkAnimationLib() {
        Plugin plugin = getServer().getPluginManager().getPlugin("AnimationLib");
        if (plugin != null) {
            String str = plugin.getDescription().getVersion();
            if (str.equals(ANIMLIB_VERSION) || Double.parseDouble("0." + str.replace(".", ""))
                    >= Double.parseDouble("0." + ANIMLIB_VERSION.replace(".", "")))
                return true;
        }
        getServer().getConsoleSender().sendMessage(new String[] {
                ChatColor.RED + "[AnimatedMenu] I require AnimationLib v" + ANIMLIB_VERSION + " to work!",
                ChatColor.RED + "Download it at \"https://www.spigotmc.org/resources/22295/\""
        });
        getServer().getPluginManager().disablePlugin(this);
        return false;
    }

    @Override
    public void onDisable() {
        // InventoryCloseEvent could cause ConcurrentModificationException
        new HashSet<>(menuRegistry.getOpenMenu().keySet()).forEach(HumanEntity::closeInventory);
        asyncTasks.forEach(BukkitTask::cancel);
        asyncTasks.clear();
        connections.cancel();
        menuRegistry.onDisable();
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

    protected MenuLoader getDefaultMenuLoader() {
        return new MenuLoader(this);
    }
    
    private void checkForUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                String current = getDescription().getVersion();
                try {
                    URLConnection connection = new URL("https://github.com/megamichiel/AnimatedMenu").openConnection();
                    Scanner scanner = new Scanner(connection.getInputStream());
                    scanner.useDelimiter("\\Z");
                    String content = scanner.next();
                    Matcher matcher = Pattern.compile("Current\\sVersion:\\s(<b>)?([0-9]\\.[0-9]\\.[0-9])(</b>)?").matcher(content);
                    if (!matcher.find()) {
                        nag("Failed to check for updates: Unknown page format!");
                        return;
                    }
                    String version = matcher.group(2);
                    update = current.equals(version) ? null : version;
                    scanner.close();
                } catch (IOException ex) {
                    nag("Failed to check for updates:");
                    nag(ex);
                }
                if (update != null)
                    getLogger().info("A new version is available! (Current version: " + current + ", new version: " + update + ")");
            }
        }.runTaskAsynchronously(this);
    }
    
    protected void registerDefaultCommandHandlers() {
        commands.clear();
        Collections.addAll(commands, new TextCommand("console") {
            @Override
            public boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value);
                return true;
            }
        }, new TextCommand("message") {
            @Override
            public boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
                p.sendMessage(value);
                return true;
            }
        }, new TextCommand("op") {
            @Override
            public boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
                boolean op = p.isOp();
                p.setOp(true);
                p.performCommand(value);
                p.setOp(op);
                return true;
            }
        }, new TextCommand("broadcast") {
            @Override
            public boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
                Bukkit.broadcastMessage(value);
                return true;
            }
        }, new TextCommand("server") {
            @Override
            public boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                try {
                    dos.writeUTF("Connect");
                    dos.writeUTF(value);
                } catch (Exception ex) {
                    plugin.nag("An error occured on trying to connect a player to '" + value + "'");
                    plugin.nag(ex);
                    return true;
                }
                p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                return true;
            }
        }, new TextCommand("menu") {
            @Override
            public boolean executeCached(AnimatedMenuPlugin plugin, Player p, String value) {
                MenuRegistry registry = plugin.getMenuRegistry();
                AnimatedMenu menu = registry.getMenu(value);
                if (menu != null) registry.openMenu(p, menu);
                else plugin.nag("No menu with name " + value + " found!");
                return true;
            }
        }, new TellRawCommand(this), new SoundCommand());
    }
    
    public boolean warnOfflineServers() {
        return warnOfflineServers;
    }
    
    protected void loadConfig() {
        AbstractConfig config = getConfiguration();
        if (config.isSection("connections")) {
            AbstractConfig section = config.getSection("connections");
            for (String key : section.keys()) {
                if (section.isSection(key)) {
                    AbstractConfig sec = section.getSection(key);
                    String ip = sec.getString("ip");
                    if (ip != null) {
                        int index = ip.indexOf(':');
                        int port;
                        if (index == -1) port = 25565;
                        else {
                            port = Integer.parseInt(ip.substring(index + 1));
                            ip = ip.substring(0, index);
                        }
                        connections.add(key.toLowerCase(ENGLISH),
                                new InetSocketAddress(ip, port)).load(sec);
                    }
                }
            }
        }
        warnOfflineServers = config.getBoolean("warn-offline-servers");
        connections.schedule(config.getLong("connection-refresh-delay", 10 * 20L));
    }
    
    void reload() {
        menuRegistry.onDisable();
        saveDefaultConfig();
        reloadConfig();

        connections.clear();
        connections.cancel();
        loadConfig();
        
        registerDefaultCommandHandlers();
        menuRegistry.loadMenus();
    }
    
    /* Listeners */
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (update != null && player.hasPermission("animatedmenu.seeupdate"))
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&6" + getDescription().getName() + "&8] &aA new version is available! (Current version: " + getDescription().getVersion() + ", new version: " + update + ")"));
        PlayerInventory inv = player.getInventory();
        for (AnimatedMenu menu : menuRegistry) {
            menu.getSettings().giveOpener(inv, false);
            if (menu.getSettings().shouldOpenOnJoin())
                post(() -> menuRegistry.openMenu(player, menu), false);
        }
    }
    
    @EventHandler
    public void on(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPermission("animatedmenu.open"))
            return;
        if (e.getItem() != null) {
            MaterialData data = e.getItem().getData();
            int amount = e.getItem().getAmount();
            ItemMeta meta = e.getItem().getItemMeta();
            for (AnimatedMenu menu : menuRegistry) {
                if (menu.getSettings().canOpenWith(data, amount, meta)) {
                    menuRegistry.openMenu(p, menu);
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        menuRegistry.closeMenu((Player) e.getPlayer());
    }
    
    @EventHandler
    public void on(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        AnimatedMenu open = menuRegistry.getOpenedMenu(p);
        if (open == null) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        InventoryView view = e.getView();
        if ((view.getTopInventory() != null) && slot >= 0
                && (slot < view.getTopInventory().getSize()))
            open.getMenuGrid().click(p, e.getClick(), e.getSlot());
    }

    @EventHandler
    public void on(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        for (Delay<Player> delay : delays)
            delay.remove(p);
    }

    public Delay<Player> addPlayerDelay(long time) {
        Delay<Player> delay = new Delay<>(time);
        delays.add(delay);
        return delay;
    }

    public OpenAnimation.Type resolveAnimationType(String name) {
        try {
            return OpenAnimation.DefaultType.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public List<Command<?, ?>> getCommands() {
        return commands;
    }

    public AnimatedMenuCommand getCommand() {
        return command;
    }

    public MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    RemoteConnections getConnections() {
        return connections;
    }

    public boolean isVaultPresent() {
        return vaultPresent;
    }

    public boolean isPlayerPointsPresent() {
        return playerPointsPresent;
    }

    @Override
    public void onClose() {}

    @Override
    public void post(Runnable task, boolean async) {
        if (async) getServer().getScheduler().runTaskAsynchronously(this, task);
        else getServer().getScheduler().runTask(this, task);
    }
}
