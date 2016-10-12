package me.megamichiel.animatedmenu.menu.item;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.command.CommandExecutor;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ItemClickListener {
    
    private static final String PERMISSION_MESSAGE = "&cYou are not permitted to do that!",
            PRICE_MESSAGE = "&cYou don't have enough money for that!",
            POINTS_MESSAGE = "&cYou don't have enough points for that!";
    
    private final List<ClickProcessor> clicks = new ArrayList<>();
    
    ItemClickListener(AnimatedMenuPlugin plugin, AbstractConfig section) {
        if (section.isSection("commands")) {
            AbstractConfig commandSection = section.getSection("commands");
            commandSection.keys().stream().filter(commandSection::isSection)
                    .map(key -> parse(plugin, commandSection.getSection(key)))
                    .forEach(clicks::add);
        } else clicks.add(parse(plugin, section));
    }

    private ClickProcessor parse(AnimatedMenuPlugin plugin, AbstractConfig section) {
        CommandExecutor commandExecutor = new CommandExecutor(plugin),
                buyCommandExecutor = new CommandExecutor(plugin);
        commandExecutor.load(plugin, section, "commands");
        buyCommandExecutor.load(plugin, section, "buy-commands");
        String click = section.getString("click-type", "both").toLowerCase(Locale.US);
        boolean rightClick = click.equals("both") || click.equals("right"),
                leftClick = click.equals("both") || click.equals("left");
        Flag shiftClick = Flag.parseFlag(section.getString("shift-click"), Flag.BOTH);
        int price = section.getInt("price", -1), points = section.getInt("points", -1);
        String permission = section.getString("permission"),
                permissionMessage = section.getString("permission-message", PERMISSION_MESSAGE),
                bypassPermission = section.getString("bypass-permission"),
                priceMessage = section.getString("price-message", PRICE_MESSAGE),
                pointsMessage = section.getString("points-message", POINTS_MESSAGE);
        CloseAction closeAction = CloseAction.NEVER;
        if (section.isString("close")) {
            String close = section.getString("close");
            try {
                closeAction = CloseAction.valueOf(close
                        .toUpperCase(Locale.ENGLISH).replace('-', '_'));
            } catch (IllegalArgumentException ex) {
                if (Flag.parseBoolean(close, false))
                    closeAction = CloseAction.ON_SUCCESS;
            }
        }
        return new ClickProcessor(plugin, commandExecutor, buyCommandExecutor,
                rightClick, leftClick, shiftClick, price, points,
                StringBundle.parse(plugin, permission),
                StringBundle.parse(plugin, permissionMessage).colorAmpersands(),
                StringBundle.parse(plugin, bypassPermission),
                StringBundle.parse(plugin, priceMessage).colorAmpersands(),
                StringBundle.parse(plugin, pointsMessage).colorAmpersands(),
                closeAction);
    }
    
    void onClick(Player who, ClickType click) {
        for (ClickProcessor cp : this.clicks) cp.onClick(who, click);
    }
    
    private class ClickProcessor {

        private final AnimatedMenuPlugin plugin;
        private final CommandExecutor commandExecutor, buyCommandExecutor;
        private final boolean rightClick, leftClick;
        private final Flag shiftClick;
        private final int price, pointPrice;
        private final StringBundle permission, permissionMessage, bypassPermission, priceMessage, pointsMessage;
        private final CloseAction closeAction;

        private ClickProcessor(AnimatedMenuPlugin plugin, CommandExecutor commandExecutor,
                               CommandExecutor buyCommandExecutor, boolean rightClick, boolean leftClick,
                               Flag shiftClick, int price, int pointPrice, StringBundle permission,
                               StringBundle permissionMessage, StringBundle bypassPermission,
                               StringBundle priceMessage, StringBundle pointsMessage,
                               CloseAction closeAction) {
            this.plugin = plugin;
            this.commandExecutor = commandExecutor;
            this.buyCommandExecutor = buyCommandExecutor;
            this.rightClick = rightClick;
            this.leftClick = leftClick;
            this.shiftClick = shiftClick;
            this.price = price;
            this.pointPrice = pointPrice;
            this.permission = permission;
            this.permissionMessage = permissionMessage;
            this.bypassPermission = bypassPermission;
            this.priceMessage = priceMessage;
            this.pointsMessage = pointsMessage;
            this.closeAction = closeAction;
        }

        void onClick(Player who, ClickType click) {
            if (((rightClick && click.isRightClick()) || (leftClick && click.isLeftClick()))
                    && shiftClick.matches(click.isShiftClick())) {
                if (bypassPermission == null || !who.hasPermission(bypassPermission.toString(who))) {
                    if (permission != null && !who.hasPermission(permission.toString(who))) {
                        who.sendMessage(permissionMessage.toString(who));
                        close(who, false);
                        return;
                    }
                    boolean bought = false;
                    if (price != -1 && plugin.isVaultPresent()
                            && !who.hasPermission("animatedmenu.economy.bypass")) {
                        Economy econ = plugin.economy;
                        if (econ.getBalance(who) >= price) {
                            econ.withdrawPlayer(who, price);
                            bought = true;
                        } else {
                            who.sendMessage(priceMessage.toString(who));
                            close(who, false);
                            return;
                        }
                    }
                    if (pointPrice != -1 && plugin.isPlayerPointsPresent()
                            && !who.hasPermission("animatedmenu.points.bypass")) {
                        if (plugin.playerPointsAPI.take(who.getUniqueId(), pointPrice)) {
                            bought = true;
                        } else {
                            who.sendMessage(pointsMessage.toString(who));
                            close(who, false);
                            return;
                        }
                    }
                    if (bought) buyCommandExecutor.execute(who);
                }
                commandExecutor.execute(who);
                close(who, true);
            }
        }

        private void close(HumanEntity who, boolean success) {
            if ((closeAction.onSuccess && success) ||
                    (closeAction.onFailure && !success))
                who.closeInventory();
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
