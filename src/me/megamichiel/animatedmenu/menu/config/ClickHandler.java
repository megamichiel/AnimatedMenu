package me.megamichiel.animatedmenu.menu.config;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.command.CommandExecutor;
import me.megamichiel.animatedmenu.util.Delay;
import me.megamichiel.animatedmenu.util.Flag;
import me.megamichiel.animatedmenu.util.PluginCurrency;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;
import java.util.stream.Stream;

public class ClickHandler {

    public static final String PERMISSION_MESSAGE = "&cYou are not permitted to do that!";

    private static final Purchase<?>[] PURCHASES = {
            new Purchase<Double>("price", "&cYou don't have enough money for that!") {
                @Override
                protected Double parse(AnimatedMenuPlugin plugin, String value) {
                    if (!PluginCurrency.VAULT.isAvailable()) return null;
                    try {
                        double d = Double.parseDouble(value);
                        return d > 0 ? d : null;
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }

                @Override
                protected boolean test(AnimatedMenuPlugin plugin, Player player, Double value) {
                    return PluginCurrency.VAULT.has(player, value);
                }

                @Override
                protected void take(AnimatedMenuPlugin plugin, Player player, Double value) {
                    PluginCurrency.VAULT.take(player, value);
                }
            },
            new Purchase<Integer>("points", "&cYou don't have enough points for that!") {
                @Override
                protected Integer parse(AnimatedMenuPlugin plugin, String value) {
                    if (!PluginCurrency.PLAYER_POINTS.isAvailable()) return null;
                    try {
                        int i = Integer.parseInt(value);
                        return i > 0 ? i : null;
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }

                @Override
                protected boolean test(AnimatedMenuPlugin plugin, Player player, Integer value) {
                    return PluginCurrency.PLAYER_POINTS.has(player, value);
                }

                @Override
                protected void take(AnimatedMenuPlugin plugin, Player player, Integer value) {
                    PluginCurrency.PLAYER_POINTS.take(player, value);
                }
            },
            new Purchase<Integer>("gems", "&cYou don't have enough gems for that!") {
                @Override
                protected Integer parse(AnimatedMenuPlugin plugin, String value) {
                    if (!PluginCurrency.GEMS.isAvailable()) return null;
                    try {
                        int i = Integer.parseInt(value);
                        return i > 0 ? i : null;
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }

                @Override
                protected boolean test(AnimatedMenuPlugin plugin, Player player, Integer value) {
                    return PluginCurrency.GEMS.has(player, value);
                }

                @Override
                protected void take(AnimatedMenuPlugin plugin, Player player, Integer value) {
                    PluginCurrency.GEMS.take(player, value);
                }
            },
            new ClickHandler.Purchase<Number>("exp", "&cYou don't have enough exp for that!") {
                @Override
                protected Number parse(AnimatedMenuPlugin plugin, String value) {
                    try {
                        if (value.charAt(0) == 'L' || value.charAt(0) == 'l') {
                            int i = Integer.parseInt(value.substring(1));
                            return i > 0 ? i : null;
                        }
                        float f = Float.parseFloat(value);
                        return f > 0f ? f : null;
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }

                @Override
                protected boolean test(AnimatedMenuPlugin plugin, Player player, Number value) {
                    if (value instanceof Float) return player.getExp() >= value.floatValue();
                    return player.getLevel() >= value.intValue();
                }

                @Override
                protected void take(AnimatedMenuPlugin plugin, Player player, Number value) {
                    if (value instanceof Float) player.setExp(player.getExp() - value.floatValue());
                    else player.setLevel(player.getLevel() - value.intValue());
                }
            }
    };

    private final List<Entry> entries = new ArrayList<>();

    public ClickHandler(MenuLoader loader, String menu, String item, AbstractConfig section) {
        Map<String, Object> values = null;
        Object o = section.get("click-handlers");
        if (o != null) {
            if (o instanceof AbstractConfig) {
                values = ((AbstractConfig) o).values();
            } else if (o instanceof Collection) {
                int i = 0;
                values = new HashMap<>();
                for (Object obj : ((Collection) o)) {
                    values.put(Integer.toString(++i), obj);
                }
            }
        } else if ((o = section.get("commands")) != null) {
            if (o instanceof AbstractConfig) {
                values = ((AbstractConfig) o).values();
            } else if (o instanceof List) {
                (values = new HashMap<>()).put("(self)", section);
            }
        }
        if (values != null) {
            values.forEach((key, value) -> {
                if (value instanceof AbstractConfig) {
                    String path = key + "_" + item + "_" + menu;
                    Entry entry = loader.parseClickHandler(path, (AbstractConfig) value);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            });
        }
    }

    public void click(Player player, ClickType type) {
        for (Entry entry : entries) entry.click(player, type);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public static class Entry {

        private final ClickPredicate click;

        private final StringBundle permission, permissionMessage, bypassPermission;
        private final PurchaseData<?>[] purchases;

        private final CommandExecutor clickExecutor;
        private final CloseAction closeAction;

        private final Delay delay;

        public Entry(String path, AnimatedMenuPlugin plugin, AbstractConfig section, Purchase<?>... purchases) {
            click = new ClickPredicate(section);
            (clickExecutor = new CommandExecutor(plugin)).load(plugin, section, "commands");
            permission = StringBundle.parse(plugin, section.getString("permission"));
            permissionMessage = StringBundle.parse(plugin, section.getString("permission-message", PERMISSION_MESSAGE)).colorAmpersands();
            bypassPermission = StringBundle.parse(plugin, section.getString("bypass-permission"));

            Purchase<?>[] allPurchases = Arrays.copyOf(PURCHASES, PURCHASES.length + purchases.length);
            System.arraycopy(purchases, 0, allPurchases, PURCHASES.length, purchases.length);

            this.purchases = Stream.of(allPurchases).map(p -> p.tryParse(plugin, section))
                    .filter(Objects::nonNull).toArray(PurchaseData[]::new);

            CloseAction closeAction = CloseAction.NEVER;
            if (section.isString("close")) {
                String close = section.getString("close");
                try {
                    closeAction = CloseAction.valueOf(close.toUpperCase(Locale.ENGLISH).replace('-', '_'));
                } catch (IllegalArgumentException ex) {
                    if (Flag.parseBoolean(close)) {
                        closeAction = CloseAction.ON_SUCCESS;
                    }
                }
            }
            this.closeAction = closeAction;

            long clickDelay = plugin.parseTime(section, "click-delay", 0) * 50L;
            if (clickDelay > 0) {
                delay = plugin.addPlayerDelay("item_" + path, section.getString("delay-message"), clickDelay);
            } else {
                delay = null;
            }
        }

        void click(Player player, ClickType type) {
            if (!click.test(type)) {
                return;
            }
            if (canClick(player)) {
                clickExecutor.accept(player);
                if (closeAction.onSuccess) {
                    player.closeInventory();
                }
            } else if (closeAction.onFailure) {
                player.closeInventory();
            }
        }

        protected boolean canClick(Player player) {
            if (permission != null && !player.hasPermission(permission.toString(player))) {
                player.sendMessage(permissionMessage.toString(player));
                return false;
            }
            if (delay != null && !delay.test(player)) return false;

            if (bypassPermission == null || !player.hasPermission(bypassPermission.toString(player))) {
                for (PurchaseData<?> purchase : purchases) {
                    if (!purchase.test(player)) return false;
                }
                for (PurchaseData<?> purchase : purchases) {
                    purchase.perform(player);
                }
                return true;
            }
            return true;
        }
    }

    private static class ClickPredicate {

        private final boolean right, left, middle;
        private final Flag shift;

        private ClickPredicate(AbstractConfig section) {
            String click = section.getString("click-type", "both").toLowerCase(Locale.ENGLISH);
            switch (click) {
                case "all": right = left = middle = true; break;
                case "both": middle = !(right = left = true); break;
                default:
                    boolean right  = false,
                            left   = false,
                            middle = false;
                    for (String s : click.split(",")) {
                        switch (s.trim()) {
                            case "right":   right = true; break;
                            case "left":     left = true; break;
                            case "middle": middle = true; break;
                        }
                    }
                    this.right  = right;
                    this.left   = left;
                    this.middle = middle;
                    break;
            }
            if (!right && !left && !middle) {
                throw new IllegalArgumentException("No click types enabled!");
            }
            this.shift = Flag.parseFlag(section.getString("shift-click"), Flag.BOTH);
        }

        boolean test(ClickType type) {
            return shift.matches(type.isShiftClick()) && (type.isRightClick() && right
                    || type.isLeftClick() && left || type == ClickType.MIDDLE && middle);
        }
    }

    public static abstract class Purchase<T> {

        final String path, defaultMessage;

        protected Purchase(String path, String defaultMessage) {
            this.path = path;
            this.defaultMessage = defaultMessage;
        }

        PurchaseData<T> tryParse(AnimatedMenuPlugin plugin, AbstractConfig config) {
            String str = config.getString(path);
            if (str != null) {
                T value = parse(plugin, str);
                if (value != null) {
                    return new PurchaseData<>(plugin, this, value, StringBundle.parse(plugin,
                            config.getString(path + "-message", defaultMessage)).colorAmpersands());
                }
            }
            return null;
        }

        protected abstract T parse(AnimatedMenuPlugin plugin, String value);
        protected abstract boolean test(AnimatedMenuPlugin plugin, Player player, T value);
        protected abstract void take(AnimatedMenuPlugin plugin, Player player, T value);
    }

    static class PurchaseData<T> {

        private final AnimatedMenuPlugin plugin;
        private final Purchase<T> purchase;
        private final T value;
        private final StringBundle message;

        PurchaseData(AnimatedMenuPlugin plugin, Purchase<T> purchase, T value, StringBundle message) {
            this.plugin = plugin;
            this.purchase = purchase;
            this.value = value;
            this.message = message;
        }

        boolean test(Player player) {
            if (purchase.test(plugin, player, value)) {
                return true;
            }
            player.sendMessage(message.toString(player));
            return false;
        }

        void perform(Player player) {
            purchase.take(plugin, player, value);
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
