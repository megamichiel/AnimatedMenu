package me.megamichiel.animatedmenu;

import static org.bukkit.ChatColor.*;

import java.util.*;

import me.megamichiel.animatedmenu.menu.AnimatedMenu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * 
 * The Animated Menu command
 *
 */
class AnimatedMenuCommand implements CommandExecutor, TabCompleter {
    
    private final String[] messages = {
            DARK_AQUA + "-=" + GOLD + "Animated Menu - Help" + DARK_AQUA + "=-",
            AQUA + "<> = required, [] = optional",
            GREEN + "/%s open <menu> [player]" + YELLOW + ": Open a specific menu",
            GREEN + "/%s item <menu> [player]" + YELLOW + ": Get a menu's menu opener",
            GREEN + "/%s reload" + YELLOW + ": Reload the plugin"
    };
    private final String[] permissions = new String[] {
            "animatedmenu.command.help",
            "animatedmenu.command.open",
            "animatedmenu.command.item",
            "animatedmenu.command.reload"
    };
    private final AnimatedMenuPlugin plugin;
    
    AnimatedMenuCommand(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    private final List<String> validArgs = Arrays.asList("reload", "open", "item");
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) {
            if (!sender.hasPermission("animatedmenu.command.help"))
                return invalid(sender, "You don't have permission for that!");
            sender.sendMessage(messages[0]);
            sender.sendMessage(messages[1]);
            for (int i = 2; i < messages.length; i++)
                if (sender.hasPermission(permissions[i - 1]))
                    sender.sendMessage(String.format(messages[i], label));
            return true;
        }
        String type = args[0].toLowerCase();
        if (!validArgs.contains(type)) {
            return invalid(sender, "Invalid subcommand, type /" + label + " for help");
        }
        if (!sender.hasPermission("animatedmenu.command." + type)) {
            return invalid(sender, "You don't have permission for that!");
        }
        switch (type.charAt(0)) {
            case 'r': // Reload
                plugin.reload();
                sender.sendMessage(DARK_GRAY + "[" + GOLD + plugin.getDescription().getName() + DARK_GRAY + "] "
                        + ChatColor.GREEN + "Plugin reloaded! "
                        + plugin.getMenuRegistry().getMenus().size() + " menu(s) loaded.");
                break;
            case 'o':case 'i': // Open/Item
                if (args.length < 2) return invalid(sender, "You must specify a menu!");
                AnimatedMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
                if (menu == null) return invalid(sender, "Couldn't find a menu by that name!");
                Player target = null;
                if (args.length > 2) {
                    if (!sender.hasPermission("animatedmenu.command." + type + ".other"))
                        return invalid(sender, "You are not permitted to do that for other players!");
                    for (Player player : Bukkit.getOnlinePlayers())
                        if (player.getName().equals(args[2])) {
                            target = player;
                            break;
                        }
                    if (target == null) return invalid(sender, "Couldn't find a player by that name!");
                } else if (sender instanceof Player) target = (Player) sender;
                else return invalid(sender, '/' + label + ' ' + type + " <menu> <player>");
                if (type.charAt(0) == 'o') plugin.getMenuRegistry().openMenu(target, menu);
                else if (menu.getSettings().getOpener() != null) target.getInventory().addItem(menu.getSettings().getOpener());
                else return invalid(sender, "That menu doesn't have a menu opener!");
                break;
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length <= 1) {
            String start = args.length == 0 ? "" : args[0].toLowerCase();
            for (String str : new String[] { "open", "item", "reload" })
                if (sender.hasPermission("animatedmenu.command." + str) && str.startsWith(start))
                    list.add(str);
        } else if (args.length == 2) {
            String type = args[0].toLowerCase();
            if ((type.equals("open") || type.equals("item"))
                    && sender.hasPermission("animatedmenu.command." + type + ".open")) {
                for (AnimatedMenu menu : plugin.getMenuRegistry()) {
                    if (!menu.getSettings().isHiddenFromCommand()
                            && menu.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        list.add(menu.getName().toLowerCase());
                    }
                }
            }
        }
        return list;
    }
    
    private boolean invalid(CommandSender sender, String msg) {
        sender.sendMessage(RED + msg);
        return true;
    }
}
