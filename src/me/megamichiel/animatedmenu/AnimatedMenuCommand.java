package me.megamichiel.animatedmenu;

import me.megamichiel.animatedmenu.menu.AnimatedMenu;
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
    private final Map<String, String> usages = new LinkedHashMap<>();
    
    AnimatedMenuCommand(AnimatedMenuPlugin plugin) {
        addHandler("open", "open <menu> [player]", "Open a specific menu", (sender, args) -> {
            if (!sender.hasPermission("animatedmenu.command.open"))
                return RED + "You don't have permission for that!";
            if (args.length < 2) return RED + "You must specify a menu!";
            AnimatedMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
            if (menu == null) return RED + "Couldn't find a menu by that name!";
            Player target;
            if (args.length > 2) {
                if (!sender.hasPermission("animatedmenu.command.open.other"))
                    return RED + "You are not permitted to do that for other players!";
                target = null;
                for (Player player : Bukkit.getOnlinePlayers())
                    if (player.getName().equalsIgnoreCase(args[2])) {
                        target = player;
                        break;
                    }
                if (target == null) return RED + "Couldn't find a player by that name!";
            } else if (sender instanceof Player) target = (Player) sender;
            else return RED + "$command open <menu> <player>";
            plugin.getMenuRegistry().openMenu(target, menu);
            return target.equals(sender) ? null : (GREEN + "Opened menu for " + target.getName() + "!");
        }, (sender, args) -> {
            List<String> list = new ArrayList<>();
            String query = args[1].toLowerCase(ENGLISH);
            String menuName;
            for (AnimatedMenu menu : plugin.getMenuRegistry())
                if (!menu.getSettings().isHiddenFromCommand() && (menuName =
                        menu.getName().toLowerCase(ENGLISH)).startsWith(query))
                    list.add(menuName);
            return list;
        });
        addHandler("item", "item <menu> [player]", "Get a menu's menu opener", (sender, args) -> {
            if (!sender.hasPermission("animatedmenu.command.open"))
                return RED + "You don't have permission for that!";
            if (args.length < 2) return RED + "You must specify a menu!";
            AnimatedMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
            if (menu == null) return RED + "Couldn't find a menu by that name!";
            Player target;
            if (args.length > 2) {
                if (!sender.hasPermission("animatedmenu.command.open.other"))
                    return RED + "You are not permitted to do that for other players!";
                target = null;
                for (Player player : Bukkit.getOnlinePlayers())
                    if (player.getName().equalsIgnoreCase(args[2])) {
                        target = player;
                        break;
                    }
                if (target == null) return RED + "Couldn't find a player by that name!";
            } else if (sender instanceof Player) target = (Player) sender;
            else return RED + "$command open <menu> <player>";
            plugin.getMenuRegistry().openMenu(target, menu);
            return target.equals(sender) ? null : (GREEN + "Opened menu for " + target.getName() + "!");
        }, (sender, args) -> {
            List<String> list = new ArrayList<>();
            String query = args[1].toLowerCase(ENGLISH);
            String menuName;
            for (AnimatedMenu menu : plugin.getMenuRegistry())
                if (!menu.getSettings().isHiddenFromCommand() && (menuName =
                        menu.getName().toLowerCase(ENGLISH)).startsWith(query))
                    list.add(menuName);
            return list;
        });
        addHandler("reload", "reload", "Reload the plugin", (sender, args) -> {
            if (!sender.hasPermission("animatedmenu.command.reload"))
                return RED + "You don't have permission for that!";
            plugin.reload();
            return DARK_GRAY + "[" + GOLD + plugin.getDescription().getName() + DARK_GRAY + "] "
                    + ChatColor.GREEN + "Plugin reloaded! "
                    + plugin.getMenuRegistry().getMenus().size() + " menu(s) loaded.";
        }, null);
    }

    public void addHandler(String arg, String usage, String description,
                            BiFunction<CommandSender, String[], Object> exec,
                            BiFunction<CommandSender, String[], List<String>> tab) {
        executors.put(arg, exec);
        if (tab != null) tabCompleters.put(arg, tab);
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
        if(args.length == 0) {
            if (!sender.hasPermission("animatedmenu.command.help"))
                return invalid(sender, "You don't have permission for that!");
            sender.sendMessage(messages);
            usages.forEach((arg, usage) -> {
                if (sender.hasPermission("animatedmenu.command." + arg))
                    sender.sendMessage(usage.replace("$command", label));
            });
            return true;
        }
        String type = args[0].toLowerCase(ENGLISH);
        if ("help".equals(type)) // Cheeky me:
            return onCommand(sender, command, label, new String[0]);
        BiFunction<CommandSender, String[], Object> exec = executors.get(type);
        if (exec != null) {
            Object result = exec.apply(sender, args);
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
            executors.keySet().stream()
                    .filter(s -> s.startsWith(start) &&
                            sender.hasPermission("animatedmenu.command" + s))
                    .forEach(list::add);
        } else if (args.length == 2) {
            String type = args[0].toLowerCase(ENGLISH);
            BiFunction<CommandSender, String[], List<String>> tab = tabCompleters.get(type);
            if (tab != null && sender.hasPermission("animatedmenu.command." + type))
                return tab.apply(sender, args);
        }
        return list;
    }
    
    private boolean invalid(CommandSender sender, String msg) {
        sender.sendMessage(RED + msg);
        return true;
    }
}
