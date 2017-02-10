package me.megamichiel.animatedmenu;

import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.util.PluginPermission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.Locale.ENGLISH;
import static org.bukkit.ChatColor.*;

/**
 * The Animated Menu command
 */
public class AnimatedMenuCommand implements TabExecutor {
    
    private final String[] messages = {
            DARK_AQUA + "-=" + GOLD + "Animated Menu - Help" + DARK_AQUA + "=-",
            AQUA + "<> = required, [] = optional"
    };
    private final Map<String, BiFunction<CommandSender, String[], Object>>   executors = new HashMap<>();
    private final Map<String, BiFunction<CommandSender, String[], List<String>>> tabCompleters = new HashMap<>();
    private final Map<String, PluginPermission> permissions = new LinkedHashMap<>();
    private final Map<String, String> usages = new HashMap<>();
    
    AnimatedMenuCommand(AnimatedMenuPlugin plugin) {
        usages.put("help", GREEN + "$command [help]: " + YELLOW + "See this help menu");
        addHandler("open", "open <menu> [player]", "Open a specific menu",
                PluginPermission.COMMAND_OPEN, (sender, args) -> {
            if (args.length < 2) return RED + "You must specify a menu!";
            AnimatedMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
            if (menu == null || menu.getSettings().isHiddenFromCommand())
                return RED + "Couldn't find a menu by that name!";
            Player target;
            if (args.length > 2) {
                if (!PluginPermission.COMMAND_OPEN_OTHER.test(sender))
                    return RED + "You are not permitted to do that for other players!";
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) return RED + "Couldn't find a player by that name!";
            } else if (sender instanceof Player) target = (Player) sender;
            else return RED + "$command open <menu> <player>";
            plugin.getMenuRegistry().openMenu(target, menu);
            return target.equals(sender) ? null : (GREEN + "Opened menu for " + target.getName() + "!");
        }, (sender, args) -> {
            List<String> list = new ArrayList<>();
            if (args.length == 2) {
                String query = args[1].toLowerCase(ENGLISH);
                String menuName;
                for (AnimatedMenu menu : plugin.getMenuRegistry())
                    if (!menu.getSettings().isHiddenFromCommand() && (menuName =
                            menu.getName().toLowerCase(ENGLISH)).startsWith(query))
                        list.add(menuName);
            } else if (args.length == 3 && PluginPermission.COMMAND_OPEN_OTHER.test(sender)) {
                String name = args[2].toLowerCase(ENGLISH);
                for (Player player : Bukkit.getOnlinePlayers())
                    if (player.getName().toLowerCase(ENGLISH).startsWith(name))
                        list.add(player.getName());
            }
            return list;
        });
        addHandler("item", "item <menu> [player]", "Get a menu's menu opener",
                PluginPermission.COMMAND_ITEM, (sender, args) -> {
            if (args.length < 2) return RED + "You must specify a menu!";
            AnimatedMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
            if (menu == null) return RED + "Couldn't find a menu by that name!";
            Player target;
            if (args.length > 2) {
                if (!PluginPermission.COMMAND_ITEM_OTHER.test(sender))
                    return RED + "You are not permitted to do that for other players!";
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) return RED + "Couldn't find a player by that name!";
            } else if (sender instanceof Player) target = (Player) sender;
            else return RED + "$command item <menu> <player>";
            menu.getSettings().giveOpener(target.getInventory(), true);
            return target.equals(sender) ? null : (GREEN + "Gave " + target.getName() + " the menu item!");
        }, (sender, args) -> {
            List<String> list = new ArrayList<>();
            if (args.length == 2) {
                String query = args[1].toLowerCase(ENGLISH);
                String menuName;
                for (AnimatedMenu menu : plugin.getMenuRegistry())
                    if (!menu.getSettings().isHiddenFromCommand() && (menuName =
                            menu.getName().toLowerCase(ENGLISH)).startsWith(query))
                        list.add(menuName);
            } else if (args.length == 3 && PluginPermission.COMMAND_ITEM_OTHER.test(sender)) {
                String name = args[2].toLowerCase(ENGLISH);
                for (Player player : Bukkit.getOnlinePlayers())
                    if (player.getName().toLowerCase(ENGLISH).startsWith(name))
                        list.add(player.getName());
            }
            return list;
        });
        addHandler("reload", "reload", "Reload the plugin",
                PluginPermission.COMMAND_RELOAD, (sender, args) -> {
            plugin.reload();
            return DARK_GRAY + "[" + GOLD + plugin.getDescription().getName() + DARK_GRAY + "] "
                    + ChatColor.GREEN + "Plugin reloaded! "
                    + plugin.getMenuRegistry().getMenus().size() + " menu(s) loaded.";
        }, null);
    }

    public void addHandler(String arg, String usage, String description, PluginPermission permission,
                            BiFunction<CommandSender, String[], Object> exec,
                            BiFunction<CommandSender, String[], List<String>> tab) {
        executors.put(arg, exec);
        if (tab != null) tabCompleters.put(arg, tab);
        permissions.put(arg, permission);
        usages.put(arg, GREEN + "$command " + usage + ": " + YELLOW + description);
    }

    public void removeHandler(String arg) {
        if (executors.remove(arg) != null) {
            tabCompleters.remove(arg);
            usages.remove(arg);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String type = args.length == 0 ? "help" : args[0].toLowerCase(ENGLISH);
        if("help".equals(type)) {
            if (!PluginPermission.COMMAND_HELP.test(sender))
                return invalid(sender, "You don't have permission for that!");
            sender.sendMessage(messages);
            permissions.forEach((arg, perm) -> {
                if (perm.test(sender))
                    sender.sendMessage(usages.get(arg).replace("$command", '/' + label));
            });
            return true;
        }
        PluginPermission perm = permissions.get(type);
        if (perm != null) {
            if (!perm.test(sender))
                return invalid(sender, "You don't have permission for that!");
            Object result = executors.get(type).apply(sender, args);
            if (result instanceof String)
                sender.sendMessage(((String) result).replace("$command", '/' + label));
            return true;
        }
        return invalid(sender, "Unknown arguments, type \"/" + label + "\" for help");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length <= 1) {
            String start = args.length == 0 ? "" : args[0].toLowerCase(ENGLISH);
            permissions.forEach((arg, perm) -> {
                if (arg.startsWith(start) && perm.test(sender))
                    list.add(arg);
            });
        } else {
            String type = args[0].toLowerCase(ENGLISH);
            BiFunction<CommandSender, String[], List<String>> tab = tabCompleters.get(type);
            if (tab != null && permissions.get(type).test(sender))
                return tab.apply(sender, args);
        }
        return list;
    }
    
    private boolean invalid(CommandSender sender, String msg) {
        sender.sendMessage(RED + msg);
        return true;
    }
}
