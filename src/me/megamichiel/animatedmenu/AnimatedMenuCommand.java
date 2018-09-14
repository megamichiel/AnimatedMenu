package me.megamichiel.animatedmenu;

import me.megamichiel.animatedmenu.menu.AbstractMenu;
import me.megamichiel.animatedmenu.menu.Menu;
import me.megamichiel.animatedmenu.util.PluginMessage;
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
import static me.megamichiel.animatedmenu.util.PluginMessage.*;
import static me.megamichiel.animatedmenu.util.PluginPermission.*;
import static org.bukkit.ChatColor.RED;

/**
 * The Animated Menu command
 */
public class AnimatedMenuCommand implements TabExecutor {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final AnimatedMenuPlugin plugin;

    AnimatedMenuCommand(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
        addHandler("help", USAGE__HELP, DESC__HELP, COMMAND_HELP, (sender, label, args) -> {
            sender.sendMessage(plugin.formatRaw(PluginMessage.HELP__HEADER).split("\n"));
            subCommands.forEach((arg, cmd) -> {
                if (cmd.permission.test(sender)) {
                    sender.sendMessage(plugin.formatRaw(PluginMessage.HELP__USAGE,
                            "usage", plugin.formatRaw(cmd.usage, "label", label),
                            "desc", plugin.formatRaw(cmd.desc)
                    ));
                }
            });
            return null;
        }, null);

        addHandler("list", USAGE__LIST, DESC__LIST, COMMAND_LIST, (sender, label, args) -> {
            StringBuilder sb = new StringBuilder();
            Player player = sender instanceof Player ? (Player) sender : null;
            for (AbstractMenu menu : plugin.getMenuRegistry().getMenus()) {
                if (menu instanceof Menu) {
                    Menu gui = (Menu) menu;

                    if (!gui.getSettings().isHiddenFromCommand()) {
                        sb.append(gui.getName());
                    } else if (player == null) {
                        sb.append(gui.getName()).append(' ').append(plugin.formatRaw(COMMAND__LIST_HIDDEN));
                    } else {
                        continue;
                    }
                    sb.append(", ");
                }
            }

            if (sb.length() == 0) {
                return plugin.format(COMMAND__LIST_NONE);
            }

            return plugin.format(COMMAND__LIST, "menus", sb.substring(0, sb.length() - 2));
        }, (sender, args) -> Collections.emptyList());

        addHandler("open", USAGE__OPEN, DESC__OPEN, COMMAND_OPEN, (sender, label, args) -> {
            if (args.length < 2) {
                return plugin.format(COMMAND__NO_MENU);
            }
            AbstractMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
            if (!(menu instanceof Menu) || ((Menu) menu).getSettings().isHiddenFromCommand()) {
                return plugin.format(MENU__NOT_FOUND, "menu", args[1]);
            }
            Player target;
            if (args.length > 2) {
                if (!COMMAND_OPEN_OTHER.test(sender)) {
                    return plugin.format(COMMAND__NO_OTHER);
                }
                if ((target = Bukkit.getPlayerExact(args[2])) == null) {
                    return plugin.format(COMMAND__PLAYER_NOT_FOUND, "player", args[2]);
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                return "usage";
            }
            ((Menu) menu).open(target);
            return target.equals(sender) ? null : plugin.format(COMMAND__OPENED, "player", target.getName());
        }, (sender, args) -> {
            if (args.length == 2) {
                String query = args[1].toLowerCase(ENGLISH), menuName;
                Player player = sender instanceof Player ? (Player) sender : null;
                List<String> list = new ArrayList<>();
                for (AbstractMenu menu : plugin.getMenuRegistry()) {
                    if (menu instanceof Menu && !((Menu) menu).getSettings().isHiddenFromCommand()
                            && ((Menu) menu).getSettings().canOpen(player) == null
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

        addHandler("item", USAGE__ITEM, DESC__ITEM, COMMAND_ITEM, (sender, label, args) -> {
            if (args.length < 2) {
                return plugin.format(COMMAND__NO_MENU);
            }
            AbstractMenu menu = plugin.getMenuRegistry().getMenu(args[1]);
            if (!(menu instanceof Menu)) {
                return plugin.format(MENU__NOT_FOUND, "menu", args[1]);
            }
            Player target;
            if (args.length > 2) {
                if (!COMMAND_ITEM_OTHER.test(sender)) {
                    return plugin.format(COMMAND__NO_OTHER);
                }
                if ((target = Bukkit.getPlayerExact(args[2])) == null) {
                    return plugin.format(COMMAND__PLAYER_NOT_FOUND, "player", args[2]);
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                return "usage";
            }
            if (!((Menu) menu).getSettings().giveOpener(target.getInventory(), true)) {
                return plugin.format(COMMAND__NO_OPENER);
            }
            return target.equals(sender) ? plugin.format(COMMAND__GOT_OPENER, "menu", menu.getName()) :
                    plugin.format(COMMAND__GAVE_OPENER, "player", target.getName(), "menu", menu.getName());
        }, (sender, args) -> {
            if (args.length == 2) {
                String query = args[1].toLowerCase(ENGLISH), menuName;
                List<String> list = new ArrayList<>();
                for (AbstractMenu menu : plugin.getMenuRegistry()) {
                    if (menu instanceof Menu && ((Menu) menu).getSettings().hasOpener() &&
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

        addHandler("reload", USAGE__RELOAD, DESC__RELOAD, COMMAND_RELOAD,
                (sender, label, args) -> plugin.format(COMMAND__RELOADED, "menus", plugin.reload()), null);
    }

    public void addHandler(String arg, PluginMessage usage, PluginMessage desc, PluginPermission permission,
                           Handler handler,
                           BiFunction<CommandSender, String[], List<String>> tab) {
        subCommands.put(arg, new SubCommand(permission, usage, desc, handler, tab));
    }

    public void removeHandler(String arg) {
        subCommands.remove(arg);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        SubCommand cmd = subCommands.get(args.length == 0 ? "help" : args[0].toLowerCase(ENGLISH));
        if (cmd != null) {
            if (!cmd.permission.test(sender)) {
                sender.sendMessage(plugin.format(COMMAND__NO_PERMISSION));
                return true;
            }
            Object result = cmd.handler.execute(sender, label, args);
            if (result != null && result.getClass() == String.class) {
                if ("usage".equals(result)) {
                    sender.sendMessage(plugin.formatRaw(PREFIX) + RED + plugin.formatRaw(cmd.usage, "label", label));
                } else {
                    sender.sendMessage((String) result);
                }
            }
            return true;
        }
        sender.sendMessage(plugin.format(COMMAND__UNKNOWN_ARGUMENTS, "label", label));
        return true;
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

    public interface Handler {
        Object execute(CommandSender sender, String label, String[] args);
    }

    private static class SubCommand {

        private final PluginPermission permission;
        private final Handler handler;
        private final BiFunction<CommandSender, String[], List<String>> tabCompleter;
        private final PluginMessage usage, desc;

        SubCommand(PluginPermission permission, PluginMessage usage, PluginMessage desc, Handler handler, BiFunction<CommandSender, String[], List<String>> tabCompleter) {
            this.permission = permission;
            this.usage = usage;
            this.desc = desc;
            this.handler = handler;
            this.tabCompleter = tabCompleter;
        }
    }
}
