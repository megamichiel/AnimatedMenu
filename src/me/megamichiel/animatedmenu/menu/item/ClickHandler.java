package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.command.CommandExecutor;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ClickHandler {

    public static final String PERMISSION_MESSAGE = "&cYou are not permitted to do that!";
    private static final String PRICE_MESSAGE = "&cYou don't have enough money for that!",
                                POINTS_MESSAGE = "&cYou don't have enough points for that!";

    private final List<Entry> entries = new ArrayList<>();

    public ClickHandler(AnimatedMenuPlugin plugin, String menu,
                        String item, AbstractConfig section) {
        Map<String, Object> values = null;
        Object o = section.get("click-handlers");
        if (o != null) {
            if (o instanceof AbstractConfig) values = ((AbstractConfig) o).values();
            else if (o instanceof Collection) {
                int i = 0;
                values = new HashMap<>();
                for (Object obj : ((Collection) o))
                    values.put(Integer.toString(++i), obj);
            }
        } else if ((o = section.get("commands")) != null) {
            if (o instanceof AbstractConfig) values = ((AbstractConfig) o).values();
            else if (o instanceof List)
                (values = new HashMap<>()).put("(self)", section);
        }
        if (values != null) values.forEach((key, value) -> {
            if (value instanceof AbstractConfig) {
                String path = key + " of item " + item + " in menu " + menu;
                Entry entry = plugin.getMenuRegistry().getMenuLoader()
                        .parseClickHandler(path, (AbstractConfig) value);
                if (entry != null) entries.add(entry);
            }
        });
    }

    public void click(Player player, ClickType type) {
        for (Entry entry : entries) entry.click(player, type);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public static class Entry {

        private final AnimatedMenuPlugin plugin;
        private final Click click;

        private final CommandExecutor clickExecutor, buyExecutor;
        private final int price, points;
        private final StringBundle permission, permissionMessage,
                bypassPermission, priceMessage, pointsMessage;
        private final CloseAction closeAction;

        private final long clickDelay;
        private final StringBundle delayMessage;
        private final Map<Player, AtomicLong> delays;
        private long clickTimeLeft;

        public Entry(AnimatedMenuPlugin plugin, AbstractConfig section) {
            this.plugin = plugin;
            boolean right, left, middle;
            String click = section.getString("click-type", "both").toLowerCase(Locale.ENGLISH);
            switch (click) {
                case "all": right = left = middle = true; break;
                case "both": middle = !(right = left = true); break;
                default:
                    right = left = middle = false;
                    for (String s : click.split(","))
                        switch (s.trim()) {
                            case "right": right = true; break;
                            case "left": left = true; break;
                            case "middle": middle = true; break;
                        }
                    break;
            }
            if (!right && !left && !middle)
                throw new IllegalArgumentException("No click types enabled!");
            this.click = new Click(right, left, middle, Flag.parseFlag(section.getString("shift-click"), Flag.BOTH));
            (clickExecutor = new CommandExecutor(plugin)).load(plugin, section, "commands");
            (buyExecutor = new CommandExecutor(plugin)).load(plugin, section, "buy-commands");
            price = section.getInt("price"); points = section.getInt("points");
            permission = StringBundle.parse(plugin, section.getString("permission"));
            permissionMessage = StringBundle.parse(plugin, section.getString("permission-message", PERMISSION_MESSAGE)).colorAmpersands();
            bypassPermission = StringBundle.parse(plugin, section.getString("bypass-permission"));
            priceMessage = StringBundle.parse(plugin, section.getString("price-message", PRICE_MESSAGE)).colorAmpersands();
            pointsMessage = StringBundle.parse(plugin, section.getString("points-message", POINTS_MESSAGE)).colorAmpersands();
            CloseAction closeAction = CloseAction.NEVER;
            if (section.isString("close")) {
                String close = section.getString("close");
                try {
                    closeAction = CloseAction.valueOf(close
                            .toUpperCase(Locale.ENGLISH).replace('-', '_'));
                } catch (IllegalArgumentException ex) {
                    if (Flag.parseBoolean(close))
                        closeAction = CloseAction.ON_SUCCESS;
                }
            }
            this.closeAction = closeAction;

            clickDelay = section.getInt("click-delay") * 50L;
            if (clickDelay > 0) {
                delayMessage = StringBundle.parse(plugin, section.getString("delay-message"));
                if (delayMessage != null) {
                    delayMessage.colorAmpersands();
                    delayMessage.replace("{secondsleft}", (n, p) -> clickTimeLeft / 1000);
                    delayMessage.replace("{allticksleft}", (n, p) -> clickTimeLeft / 50);
                    delayMessage.replace("{ticksleft}", (n, p) -> (clickTimeLeft / 50) % 20);
                }
                plugin.addPlayerMap(delays = new HashMap<>());
            } else {
                delays = null;
                delayMessage = null;
            }
        }

        void click(Player player, ClickType type) {
            if (!click.matches(type)) return;
            if (canClick(player)) {
                if (clickDelay > 0) {
                    long time = System.currentTimeMillis();
                    AtomicLong al = delays.computeIfAbsent(player, p -> new AtomicLong());
                    if (time < al.get()) {
                        if (delayMessage != null) {
                            clickTimeLeft = al.get() - time;
                            player.sendMessage(delayMessage.toString(player));
                        }
                        return;
                    }
                    al.set(time + clickDelay);
                }
                clickExecutor.execute(player);
                if (closeAction.onSuccess) player.closeInventory();
            } else if (closeAction.onFailure) player.closeInventory();
        }

        protected boolean canClick(Player player) {
            if (permission != null && !player.hasPermission(permission.toString(player))) {
                player.sendMessage(permissionMessage.toString(player));
                return false;
            }
            if (bypassPermission == null || !player.hasPermission(bypassPermission.toString(player))) {
                boolean bought = false;
                if (price > 0 && plugin.isVaultPresent()) {
                    if (!plugin.economy.has(player, price)) { // Only check, must wait to see points
                        player.sendMessage(priceMessage.toString(player));
                        return false;
                    }
                    bought = true;
                }
                if (points > 0 && plugin.isPlayerPointsPresent()) {
                    if (!plugin.playerPointsAPI.take(player.getUniqueId(), points)) {
                        player.sendMessage(pointsMessage.toString(player));
                        return false;
                    }
                    if (bought) plugin.economy.withdrawPlayer(player, price);
                    else bought = true;
                } else if (bought) plugin.economy.withdrawPlayer(player, price);
                if (bought) buyExecutor.execute(player);
                return true;
            }
            return true;
        }
    }

    private static class Click {

        private final boolean right, left, middle;
        private final Flag shift;

        private Click(boolean right, boolean left, boolean middle, Flag shift) {
            this.right = right;
            this.left = left;
            this.middle = middle;
            this.shift = shift;
        }

        boolean matches(ClickType type) {
            return shift.matches(type.isShiftClick()) && (type.isRightClick() && right
                    || type.isLeftClick() && left || type == ClickType.MIDDLE && middle);
        }
    }

    private enum CloseAction {

        ALWAYS(true, true),
        ON_SUCCESS(true, false),
        ON_FAILURE(false, true),
        NEVER(false, false);

        private final boolean onSuccess, onFailure;

        CloseAction(boolean onSuccess, boolean onFailure) {
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }
    }
}
