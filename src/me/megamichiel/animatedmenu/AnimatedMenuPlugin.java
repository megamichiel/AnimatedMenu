package me.megamichiel.animatedmenu;

import me.megamichiel.animatedmenu.command.Command;
import me.megamichiel.animatedmenu.command.SoundCommand;
import me.megamichiel.animatedmenu.command.TellRawCommand;
import me.megamichiel.animatedmenu.command.TextCommand;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.menu.MenuLoader;
import me.megamichiel.animatedmenu.util.FormulaPlaceholder;
import me.megamichiel.animatedmenu.util.RemoteConnections;
import me.megamichiel.animatedmenu.util.RemoteConnections.ServerInfo;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.codec.Charsets;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnimatedMenuPlugin extends JavaPlugin implements Listener, Nagger {

    protected final List<Command<?, ?>> commands = new ArrayList<>();

    private final MenuRegistry menuRegistry = new MenuRegistry(this);
    
    private final Map<String, FormulaPlaceholder> formulaPlaceholders = new HashMap<>();
    private final RemoteConnections connections = new RemoteConnections(this);
    private boolean warnOfflineServers = true;
    
    private final List<BukkitTask> asyncTasks = new ArrayList<>();
    
    private String update;
    private boolean vaultPresent = false, playerPointsPresent = false;
    public Economy economy;
    public PlayerPointsAPI playerPointsAPI;
    
    @Override
    public void onEnable() {
        if (!requirePlugin("AnimationLib", "1.1.1", "https://www.spigotmc.org/resources/22295/")) {
            return;
        } else {
            try {
                Class.forName("me.megamichiel.animationlib.AnimLib");
            } catch (ClassNotFoundException ex) {
                getServer().getConsoleSender().sendMessage(new String[]{
                        ChatColor.RED + "It appears you have the wrong AnimationLib plugin!",
                        ChatColor.RED + "Download the correct one at https://www.spigotmc.org/resources/22295/"
                });
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        /* Listeners */
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getCommand("animatedmenu").setExecutor(new AnimatedMenuCommand(this));
        
        /* Config / API */
        saveDefaultConfig();
        
        registerDefaultCommandHandlers();
        try
        {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            AnimatedMenuPlaceholders.register(this);
        } catch (Exception ex) {
            // No PlaceholderAPI ;c
        }
        try {
            Class.forName("net.milkbowl.vault.economy.Economy");
            economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
            vaultPresent = true;
        } catch (Exception ex) {
            //No Vault
        }
        Plugin pp = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (pp instanceof PlayerPoints) {
            playerPointsAPI = ((PlayerPoints) pp).getAPI();
            playerPointsPresent = true;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                menuRegistry.loadMenus();
            }
        }.runTask(this);
        
        /* Other Stuff */
        checkForUpdate();
        loadConfig();
        
        asyncTasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(this, menuRegistry, 0, 0));
    }

    private boolean requirePlugin(String name, String version, String url) {
        Plugin plugin = getServer().getPluginManager().getPlugin(name);
        if (plugin != null) {
            String str = plugin.getDescription().getVersion();
            if (str.equals(version)) return true;
            String[] current = str.split("\\."), required = version.split("\\.");
            int length = current.length;
            for (int x = 0; x < length; x++) {
                int a = Integer.parseInt(current[x]), b = Integer.parseInt(required[x]);
                if (a < b) break;
                if (a > b || x + 1 == length) return true;
            }
        }
        getServer().getConsoleSender().sendMessage(new String[]{
                ChatColor.RED + "I require " + name + " v" + version + " to work!",
                ChatColor.RED + "Download it at " + url
        });
        getServer().getPluginManager().disablePlugin(this);
        return false;
    }

    @Override
    public void onDisable() {
        for (Player p : new HashSet<>(menuRegistry.getOpenMenu().keySet())) { // InventoryCloseEvent could cause ConcurrentModificationException
            p.closeInventory();
        }
        for (BukkitTask task : asyncTasks)
            task.cancel();
        asyncTasks.clear();
        connections.cancel();
        menuRegistry.onDisable();
    }

    protected MenuLoader getDefaultMenuLoader() {
        return new MenuLoader();
    }
    
    private void checkForUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                String current = getDescription().getVersion();
                try
                {
                    URLConnection connection = new URL("https://github.com/megamichiel/AnimatedMenu").openConnection();
                    Scanner scanner = new Scanner(connection.getInputStream());
                    scanner.useDelimiter("\\Z");
                    String content = scanner.next();
                    Matcher matcher = Pattern.compile("Current\\sVersion:\\s(<b>)?([0-9]\\.[0-9]\\.[0-9])(</b>)?").matcher(content);
                    if (!matcher.find()) {
                        nag("Failed to check for updates: Unknown page format!");
                    }
                    String version = matcher.group(2);
                    update = current.equals(version) ? null : version;
                    scanner.close();
                }
                catch (Exception ex)
                {
                    nag("Failed to check for updates:");
                    nag(ex);
                }
                if (update != null)
                {
                    getLogger().info("A new version is available! (Current version: " + current + ", new version: " + update + ")");
                }
            }
        }.runTaskAsynchronously(this);
    }
    
    protected void registerDefaultCommandHandlers() {
        commands.clear();
        commands.addAll(Arrays.asList(new TextCommand("console") {
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
        }, new TellRawCommand(), new SoundCommand()));
    }
    
    public boolean warnOfflineServers() {
        return warnOfflineServers;
    }
    
    protected void loadConfig() {
        if (getConfig().isConfigurationSection("formulas")) {
            ConfigurationSection section = getConfig().getConfigurationSection("formulas");
            for (String key : section.getKeys(false)) {
                String val = section.getString(key);
                if (val != null) {
                    formulaPlaceholders.put(key.toLowerCase(), new FormulaPlaceholder(this, val));
                }
            }
        }
        if (getConfig().isConfigurationSection("connections")) {
            ConfigurationSection section = getConfig().getConfigurationSection("connections");
            for (String key : section.getKeys(false)) {
                if (section.isConfigurationSection(key)) {
                    ConfigurationSection sec = section.getConfigurationSection(key);
                    String ip = sec.getString("ip");
                    if (ip != null) {
                        int colonIndex = ip.indexOf(':');
                        int port = colonIndex == -1 ? 25565 : NumberConversions.toInt(ip.substring(colonIndex + 1));
                        if (colonIndex > -1)
                            ip = ip.substring(0, colonIndex);
                        ServerInfo serverInfo = connections.add(key.toLowerCase(), new InetSocketAddress(ip, port));
                        Map<StringBundle, StringBundle> map = serverInfo.getValues();
                        for (String key2 : sec.getKeys(false)) {
                            if (!key2.equals("ip")) {
                                String val = sec.getString(key2);
                                if (val != null)
                                    map.put(StringBundle.parse(this, key2).colorAmpersands(),
                                            StringBundle.parse(this, val).colorAmpersands());
                            }
                        }
                    }
                }
            }
        }
        warnOfflineServers = getConfig().getBoolean("warn-offline-servers");
        connections.schedule(getConfig().getLong("connection-refresh-delay", 10 * 20L));
    }
    
    void reload() {
        menuRegistry.onDisable();
        reloadConfig();
        
        formulaPlaceholders.clear();
        connections.clear();
        connections.cancel();
        loadConfig();
        
        registerDefaultCommandHandlers();
        menuRegistry.loadMenus();
    }
    
    /* Listeners */
    
    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerJoin(PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        if (update != null && player.hasPermission("animatedmenu.seeupdate")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&6" + getDescription().getName() + "&8] &aA new version is available! (Current version: " + getDescription().getVersion() + ", new version: " + update + ")"));
        }
        for (final AnimatedMenu menu : menuRegistry) {
            if (menu.getSettings().getOpener() != null && menu.getSettings().getOpenerJoinSlot() > -1) {
                player.getInventory().setItem(menu.getSettings().getOpenerJoinSlot(), menu.getSettings().getOpener());
            }
            if (menu.getSettings().shouldOpenOnJoin()) {
                getServer().getScheduler().runTask(this, new Runnable() {
                    @Override
                    public void run() {
                        menuRegistry.openMenu(player, menu);
                    }
                });
            }
        }
    }
    
    @EventHandler
    void onPlayerInteract(PlayerInteractEvent e) {
        if (!e.getPlayer().hasPermission("animatedmenu.open"))
            return;
        if (e.getItem() != null) {
            for (AnimatedMenu menu : menuRegistry) {
                ItemStack item = menu.getSettings().getOpener();
                if (item != null && item.getData().equals(e.getItem().getData())) {
                    ItemMeta meta = e.getItem().getItemMeta();
                    ItemMeta meta1 = item.getItemMeta();
                    if (menu.getSettings().hasOpenerName() && notEqual(meta.getDisplayName(), meta1.getDisplayName()))
                        continue;
                    if (menu.getSettings().hasOpenerLore() && notEqual(meta.getLore(), meta1.getLore()))
                        continue;
                    menuRegistry.openMenu(e.getPlayer(), menu);
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    private boolean notEqual(Object o1, Object o2) {
        return o1 == null ? o2 != null : !o1.equals(o2);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String cmd = e.getMessage().substring(1).split(" ")[0].toLowerCase();
        for (AnimatedMenu menu : menuRegistry) {
            String[] commands = menu.getSettings().getOpenCommands();
            if (commands == null) continue;
            for (String command : commands) {
                if (cmd.equals(command)) {
                    menuRegistry.openMenu(p, menu);
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        AnimatedMenu menu = menuRegistry.getOpenMenu().remove(e.getPlayer());
        if (menu != null) menu.handleMenuClose((Player) e.getPlayer());
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e)
    {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        AnimatedMenu open = menuRegistry.getOpenedMenu(p);
        if (open == null) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        InventoryView view = e.getView();
        if ((view.getTopInventory() != null) && slot >= 0 && (slot < view.getTopInventory().getSize())) {
            open.getMenuGrid().click(p, e.getClick(), e.getSlot());
        }
    }
    
    @Override
    public void nag(String message) {
        getLogger().warning(message);
    }
    
    @Override
    public void nag(Throwable throwable) {
        getLogger().warning(throwable.getClass().getName() + ": " + throwable.getMessage());
    }

    public List<Command<?, ?>> getCommands() {
        return commands;
    }

    public MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    public Map<String, FormulaPlaceholder> getFormulaPlaceholders() {
        return formulaPlaceholders;
    }

    public RemoteConnections getConnections() {
        return connections;
    }

    public boolean isVaultPresent() {
        return vaultPresent;
    }

    public boolean isPlayerPointsPresent() {
        return playerPointsPresent;
    }

    private final File configFile = new File(getDataFolder(), "config.yml");
    private YamlConfig config;

    @Override
    public void reloadConfig() {
        this.config = YamlConfig.loadConfig(this.configFile);
        InputStream defConfigStream = this.getResource("config.yml");
        if(defConfigStream != null) {
            this.config.setDefaults(YamlConfig.loadConfig(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
        }
    }

    @Override
    public YamlConfig getConfig() {
        if (config == null) reloadConfig();
        return config;
    }
}
