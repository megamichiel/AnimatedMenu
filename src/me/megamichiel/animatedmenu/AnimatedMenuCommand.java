package me.megamichiel.animatedmenu;

import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animatedmenu.menu.Menu;
import me.megamichiel.animatedmenu.util.PluginPermission;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;
import static me.megamichiel.animatedmenu.util.PluginPermission.*;
import static org.bukkit.ChatColor.*;

/**
 * The Animated Menu command
 */
public class AnimatedMenuCommand implements TabExecutor {
    
    private final String[] helpHeader = {
            DARK_AQUA + "-=" + GOLD + "Animated Menu - Help" + DARK_AQUA + "=-",
            AQUA + "<> = required, [] = optional"
    };
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    @SuppressWarnings("deprecation")
    AnimatedMenuCommand(AnimatedMenuPlugin plugin) {
        addHandler("help", "help", "See this help menu", COMMAND_HELP, (sender, args) -> {
            sender.sendMessage(helpHeader);
            subCommands.forEach((arg, cmd) -> {
                if (cmd.permission.test(sender)) {
                    sender.sendMessage(GREEN.toString() + "/animatedmenu" + cmd.usage);
                }
            });
            return null;
        }, null);
        addHandler("list", "list", "List the menus you can open", COMMAND_LIST, (sender, args) -> {
            StringBuilder sb = new StringBuilder();
            Player player = sender instanceof Player ? (Player) sender : null;
            for (AbstractMenu menu : plugin.getMenuRegistry().getMenus()) {
                if (menu instanceof Menu) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    Menu gui = (Menu) menu;
                    if (!gui.getSettings().isHiddenFromCommand()) {
                        sb.append(gui.getName());
                    } else if (player == null) {
                        sb.append(gui.getName()).append(" (hidden)");
                    }
                }
            }

            return GREEN  + "Menus: " + sb.toString();
        }, (sender, args) -> Collections.emptyList());
        addHandler("open", "open <menu> [player]", "Open a specific menu", COMMAND_OPEN, (sender, args) -> {
            if (args.length < 2) return RED + "You must specify a menu!";
            AbstractMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
            if (!(menu instanceof Menu) || ((Menu) menu).getSettings().isHiddenFromCommand()) {
                return RED + "Couldn't find a menu by that name!";
            }
            Player target;
            if (args.length > 2) {
                if (!COMMAND_OPEN_OTHER.test(sender)) {
                    return RED + "You are not permitted to do that for other players!";
                }
                target = Bukkit.getPlayerExact(args[2]);
                if (target == null) return RED + "Couldn't find a player by that name!";
            } else if (sender instanceof Player) target = (Player) sender;
            else return RED + "$command open <menu> <player>";
            ((Menu) menu).open(target);
            return target.equals(sender) ? null : (GREEN + "Opened menu for " + target.getName() + "!");
        }, (sender, args) -> {
            if (args.length == 2) {
                String query = args[1].toLowerCase(ENGLISH), menuName;
                Player player = sender instanceof Player ? (Player) sender : null;
                List<String> list = new ArrayList<>();
                for (AbstractMenu menu : plugin.getMenuRegistry()) {
                    if (menu instanceof Menu && !((Menu) menu).getSettings().isHiddenFromCommand()
                            && ((Menu) menu).canOpen(player) == null
                            && (menuName = menu.getName().toLowerCase(ENGLISH)).startsWith(query)) {
                        list.add(menuName);
                    }
                }
                return list;
            } else if (args.length == 3 && COMMAND_OPEN_OTHER.test(sender)) {
                String name = args[2].toLowerCase(ENGLISH);
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName).filter(n -> n.toLowerCase(ENGLISH).startsWith(name))
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        });
        addHandler("item", "item <menu> [player]", "Get a menu's menu opener", COMMAND_ITEM, (sender, args) -> {
            if (args.length < 2) return RED + "You must specify a menu!";
            AbstractMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
            if (menu == null || !(menu instanceof Menu)) {
                return RED + "Couldn't find a menu by that name!";
            }
            Player target;
            if (args.length > 2) {
                if (!COMMAND_ITEM_OTHER.test(sender)) {
                    return RED + "You are not permitted to do that for other players!";
                }
                if ((target = Bukkit.getPlayerExact(args[2])) == null) {
                    return RED + "Couldn't find a player by that name!";
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                return RED + "$command item <menu> <player>";
            }
            if (!((Menu) menu).getSettings().giveOpener(target.getInventory(), true)) {
                return RED + "That menu doesn't have an opener!";
            }
            return GREEN + (target.equals(sender) ? "Gave yourself the menu's item" : "Gave " + target.getName() + " the menu's item!");
        }, (sender, args) -> {
            if (args.length == 2) {
                String query = args[1].toLowerCase(ENGLISH), menuName;
                List<String> list = new ArrayList<>();
                for (AbstractMenu menu : plugin.getMenuRegistry()) {
                    if (menu instanceof Menu && !((Menu) menu).getSettings().hasOpener() &&
                            (menuName = menu.getName().toLowerCase(ENGLISH)).startsWith(query)) {
                        list.add(menuName);
                    }
                }
                return list;
            } else if (args.length == 3 && COMMAND_OPEN_OTHER.test(sender)) {
                String name = args[2].toLowerCase(ENGLISH);
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName).filter(n -> n.toLowerCase(ENGLISH).startsWith(name))
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        });
        addHandler("reload", "reload", "Reload the plugin", COMMAND_RELOAD, (sender, args) -> {
            plugin.reload();
            return DARK_GRAY + "[" + GOLD + plugin.getDescription().getName() + DARK_GRAY + "] "
                    + GREEN + "Plugin reloaded! "
                    + plugin.getMenuRegistry().getMenus().size() + " menu(s) loaded.";
        }, null);
    }

    public void addHandler(String arg, String usage, String description, PluginPermission permission,
                            BiFunction<CommandSender, String[], Object> exec,
                            BiFunction<CommandSender, String[], List<String>> tab) {
        subCommands.put(arg, new SubCommand(permission, ' ' + usage + ": " + YELLOW + description, exec, tab));
    }

    public void removeHandler(String arg) {
        subCommands.remove(arg);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        SubCommand cmd = subCommands.get(args.length == 0 ? "help" : args[0].toLowerCase(ENGLISH));
        if (cmd != null) {
            if (!cmd.permission.test(sender)) {
                return invalid(sender, "You don't have permission for that!");
            }
            Object result = cmd.executor.apply(sender, args);
            if (result != null && result.getClass() == String.class) {
                if ("usage".equals(result)) {
                    sender.sendMessage(RED + label + cmd.usage);
                } else {
                    sender.sendMessage(((String) result).replace("$command", '/' + label));
                }
            }
            return true;
        }
        return invalid(sender, "Unknown arguments, type \"/" + label + "\" for help");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length <= 1) {
            String start = args.length == 0 ? "" : args[0];
            subCommands.forEach((arg, cmd) -> {
                if (start.length() <= arg.length() && arg.regionMatches(true, 0, start, 0, start.length()) && cmd.permission.test(sender)) {
                    list.add(start + arg.substring(start.length()));
                }
            });
        } else {
            SubCommand cmd = subCommands.get(args[0].toLowerCase(ENGLISH));
            if (cmd != null && cmd.tabCompleter != null && cmd.permission.test(sender)) {
                return cmd.tabCompleter.apply(sender, args);
            }
        }
        return list;
    }
    
    private boolean invalid(CommandSender sender, String msg) {
        sender.sendMessage(RED + msg);
        return true;
    }

    private static class SubCommand {

        private final PluginPermission permission;
        private final BiFunction<CommandSender, String[], Object> executor;
        private final BiFunction<CommandSender, String[], List<String>> tabCompleter;
        private final String usage;

        SubCommand(PluginPermission permission, String usage, BiFunction<CommandSender, String[], Object> executor, BiFunction<CommandSender, String[], List<String>> tabCompleter) {
            this.permission = permission;
            this.usage = usage;
            this.executor = executor;
            this.tabCompleter = tabCompleter;
        }
    }
}
