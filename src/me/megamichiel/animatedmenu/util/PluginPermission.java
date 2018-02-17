package me.megamichiel.animatedmenu.util;

import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.Locale;
import java.util.function.Predicate;

import static org.bukkit.permissions.PermissionDefault.OP;
import static org.bukkit.permissions.PermissionDefault.TRUE;

public enum PluginPermission implements Predicate<Permissible> {

    COMMAND_HELP("Be able to view the command help menu", TRUE),
    COMMAND_RELOAD("Use /animatedmenu reload", OP),
    COMMAND_LIST("Use /animatedmenu list", TRUE),
    COMMAND_OPEN("Use /animatedmenu open <menu>", TRUE),
    COMMAND_ITEM("Use /animatedmenu item <menu>", OP),

    COMMAND_OPEN_OTHER("Make other players open menus", OP),
    COMMAND_ITEM_OTHER("Give a menu opener item to another player", OP),

    OPEN_WITH_ITEM("openWithItem", "Open menus using their opener", TRUE),
    SEE_UPDATE("seeUpdate", "Receive a message on join when an update is available", OP),

    PLUS_COMMAND_TOGGLEPI("Use /animatedmenu togglepi", OP),
    PLUS_COMMAND_RLBUNGEE("Use /animatedmenu rlbungee", OP),
    PLUS_COMMAND_TRIGGER("Use /animatedemenu trigger", OP),
    PLUS_POLLS_CLOSE_OTHER("polls.closeOther", "Close other people's polls", OP),
    PLUS_POLLS_REMOVE_OTHER("polls.removeOther", "Remove other people's polls", OP);

    private final Permission permission;
    private final boolean premium;

    PluginPermission(String name, String desc, PermissionDefault def) {
        permission = new Permission("animatedmenu." + name, desc, def);
        premium = name().startsWith("PLUS_");
    }

    PluginPermission(String desc, PermissionDefault def) {
        String name = name();
        if (premium = name().startsWith("PLUS_"))
            name = name.substring(5);
        permission = new Permission("animatedmenu." +
                name.toLowerCase(Locale.ENGLISH).replace('_', '.'), desc, def);
    }

    public boolean isPremium() {
        return premium;
    }

    public boolean test(Permissible permissible) {
        return permissible.hasPermission(permission);
    }

    public void register(PluginManager pm) {
        pm.addPermission(permission);
    }

    public void unregister(PluginManager pm) {
        pm.removePermission(permission);
    }
}
