package me.megamichiel.animatedmenu.util;

import org.bukkit.ChatColor;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public enum PluginMessage {

    PREFIX("&8[&6AnimatedMenu&8] "),
    HELP__HEADER("&3-=&6Animated Menu - Help&3=-\n&b<> = required, [] = optional"),
    HELP__USAGE("&a{usage}: &e{desc}"),

    USAGE__HELP("/{label} help"),
    USAGE__LIST("/{label} list"),
    USAGE__OPEN("/{label} open <menu> [player]"),
    USAGE__ITEM("/{label} item <menu> [player]"),
    USAGE__RELOAD("/{label} reload"),
    USAGE__TRIGGER("/{label} trigger <add [menu]|remove>"),
    USAGE__TOGGLEPI("/{label} togglepi"),

    DESC__HELP("See this help menu"),
    DESC__LIST("View the menus you can open"),
    DESC__OPEN("Open a specific menu"),
    DESC__ITEM("Get a menu's opener"),
    DESC__RELOAD("Reload the plugin"),
    DESC__TRIGGER("Add or remove a menu open trigger to/from an entity/block"),
    DESC__TOGGLEPI("Toggle Player Menu bypassing"),

    MENU__COMMAND_ARGS("&cToo many arguments!"),
    MENU__NOT_FOUND("&cNo menu with name '{menu}' found!"),

    COMMAND__NOT_PLAYER("&cYou must be a player for that!"),
    COMMAND__NO_PERMISSION("&cYou don't have permission for that!"),
    COMMAND__UNKNOWN_ARGUMENTS("&cUnknown arguments! Type '/{label}' for help."),
    COMMAND__TOO_MANY_ARGUMENTS("&cToo many arguments!"),

    COMMAND__LIST("&aMenus: {menus}"),
    COMMAND__LIST_HIDDEN("(hidden)"),
    COMMAND__LIST_NONE("&cNo menus found :("),
    COMMAND__NO_MENU("&cYou must specify a menu!"),

    COMMAND__NO_OTHER("&cYou do not have permission to do that for other players!"),
    COMMAND__PLAYER_NOT_FOUND("&cPlayer '{player}' not found!"),
    COMMAND__OPENED("&aOpened menu for {player}!"),

    COMMAND__NO_OPENER("&cThat menu doesn't have an opener!"),
    COMMAND__GOT_OPENER("&aGave yourself the opener of {menu}!"),
    COMMAND__GAVE_OPENER("&aGave {player} the opener of {menu}!"),

    COMMAND__RELOADED("&aPlugin reloaded! {menus} menu(s) loaded."),

    PLAYER_MENU__ON("&aPlayer Menu &7has been turned &aon&7!"),
    PLAYER_MENU__OFF("&aPlayer Menu &7has been turned &coff&7!"),

    ADMIN_MENU__USAGE("&c/{label} <player>"),
    ADMIN_MENU__USAGE_OPTIONAL("&c/{label} [player]"),
    ADMIN_MENU__LEFT("&cTarget player has left!"),

    TRIGGER__ADD("&aPlease right click the entity or block you want to open the menu with"),
    TRIGGER__REMOVE("&aPlease right click the entity or block you want to remove the trigger from"),

    TRIGGER__ENTITY_ADDED("&aThis entity now opens {menu}!"),
    TRIGGER__ENTITY_REMOVED("&aThis entity no longer opens {menu}!"),
    TRIGGER__ENTITY_NONE("&cThis entity does not open a menu!"),
    TRIGGER__BLOCK_ADDED("&aThis block now opens {menu}!"),
    TRIGGER__BLOCK_REMOVED("&aThis block non longer opens {menu}"),
    TRIGGER__BLOCK_NONE("&cThis block does not open a menu!"),

    POLL__HELP_HEADER("&b>> &aPoll Help"),
    POLL__HELP_USAGE("&c{usage}: &e{desc}"),

    POLL__USAGE__CREATE("/{label} create <title...>"),
    POLL__USAGE__CLOSE("/{label} close <id>"),
    POLL__USAGE__REMOVE("/{label} remove <id>"),
    POLL__USAGE__LIST("/{label} list <page>"),
    POLL__USAGE__OPEN("/{label} <id>"),

    POLL__DESC__CREATE("Create a new poll"),
    POLL__DESC__CLOSE("Close a poll"),
    POLL__DESC__REMOVE("Remove a poll"),
    POLL__DESC__LIST("View a list of polls"),
    POLL__DESC__OPEN("Open a poll"),

    POLL__LIMIT("&cYou cannot create any more polls!"),
    POLL__NOT_FOUND("&cNo poll with id '{id}' found!"),
    POLL__CANNOT_REMOVE("&cYou are not allowed to remove that poll!"),
    POLL__CANNOT_CLOSE("&cYou are not allowed to close that poll!"),
    POLL__CLOSED("&aPoll closed!"),
    POLL__ALREADY_CLOSED("&cThat poll has already been closed!"),
    POLL__REMOVED("&aPoll removed!"),
    POLL__ALREADY_REMOVED("&cThat poll has already been removed!"),
    POLL__NONE("&cThere are no polls!"),
    POLL__INVALID_PAGE("&cInvalid page '{page}'!"),
    POLL__PAGE_HEADER("&b>> &aPolls &7(page &a{page}&7)"),
    POLL__PAGE_ENTRY("&c{id}: &e{title}");

    private static final byte[] INDENT = { ' ', ' ' };

    private final String defaultValue;
    private final String[] configPath;

    PluginMessage(String defaultValue) {
        this.defaultValue = defaultValue;

        String name = name();
        StringBuilder sb = new StringBuilder(name.length()).append(name.charAt(0));

        for (int len = name.length(), i = 1, c; i < len; ++i) {
            switch (c = name.charAt(i)) {
                case '_':
                    if ((c = name.charAt(++i)) == '_') {
                        sb.append('.').append(name.charAt(++i));
                        break;
                    } else {
                        sb.append('-').append((char) c);
                        break;
                    }
                default:
                    sb.append((char) Character.toLowerCase(c));
            }
        }

        configPath = sb.toString().split("\\.");
    }

    public String getDefault() {
        return defaultValue;
    }

    public static byte[] defaultConfig() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes;

        Deque<String> path = new ArrayDeque<>();

        for (PluginMessage msg : values()) {
            String[] split = msg.configPath;
            int i = 0, length = split.length;
            for (Iterator<String> it = path.iterator(); it.hasNext() && i + 1 < length; ) {
                if (!it.next().equals(split[i++])) {
                    for (it.remove(); it.hasNext(); it.remove()) {
                        it.next();
                    }
                    break;
                }
            }
            while (i + 1 < length) {
                for (int k = 0; k < i; ++k) {
                    out.write(INDENT, 0, 2);
                }
                path.add(split[i]);
                out.write(bytes = (split[i++] + ":\n").getBytes(StandardCharsets.UTF_8), 0, bytes.length);
            }
            for (i = length - 1; i > 0; --i) {
                out.write(INDENT, 0, 2);
            }
            out.write(bytes = (split[length - 1] + ": '" + msg.defaultValue.replace("\n", "\\n").replace("'", "''") + "'\n").getBytes(StandardCharsets.UTF_8), 0, bytes.length);
        }
        return out.toByteArray();
    }

    public static class FormatText {

        private final Object[] values;

        public FormatText(String message, Object... args) {
            List<Object> list = new ArrayList<>();
            StringBuilder sb = new StringBuilder();

            boolean escape = false;
            for (int len = message.length(), i = 0, c; i < len; ) {
                switch (c = message.charAt(i++)) {
                    case '&':
                        if (escape) {
                            escape = false;
                            sb.append('&');
                        } else if (i < len && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c = message.charAt(i)) >= 0) {
                            sb.append(ChatColor.COLOR_CHAR).append((char) Character.toLowerCase(c));
                            ++i;
                        }
                        break;
                    case '\\':
                        if (!(escape = !escape)) {
                            sb.append('\\');
                        }
                        break;
                    case '{':
                        if (escape) {
                            escape = false;
                            sb.append('{');
                            break;
                        }
                        if (sb.length() > 0) {
                            list.add(sb.toString());
                            sb.setLength(0);
                        }
                        while (i < len && ((c = message.charAt(i++)) != '}' || escape)) {
                            switch (c) {
                                case '\\':
                                    if (escape = !escape) {
                                        break;
                                    }
                                default:
                                    sb.append((char) c);
                            }
                        }
                        String key = sb.toString();
                        sb.setLength(0);
                        boolean found = false;
                        for (int j = 0; j < args.length; j += 2) {
                            if (args[j].equals(key)) {
                                list.add(j + 1);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            list.add('{' + key + '}');
                        }
                        break;
                    default:
                        if (escape) {
                            escape = false;
                            switch (c) {
                                case 'n':
                                    sb.append('\n');
                                    break;
                                case 'u':
                                    if (i + 4 < len) {
                                        try {
                                            sb.append((char) Integer.parseInt(sb.substring(i, i + 4)));
                                            break;
                                        } catch (NumberFormatException ex) {
                                            // Fall-through
                                        }
                                    }
                                default:
                                    sb.append((char) c);
                            }
                        } else {
                            sb.append((char) c);
                        }
                }
            }
            if (sb.length() > 0) {
                list.add(sb.toString());
            }
            values = list.toArray();
        }

        public String format(Object... args) {
            StringBuilder sb = new StringBuilder();
            for (Object arg : values) {
                if (arg.getClass() == String.class) {
                    sb.append((String) arg);
                } else {
                    sb.append(args[(int) arg]);
                }
            }
            return sb.toString();
        }
    }
}
