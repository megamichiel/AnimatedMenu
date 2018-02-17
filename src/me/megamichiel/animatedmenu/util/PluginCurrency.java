package me.megamichiel.animatedmenu.util;

import me.JohnCrafted.gemseconomy.economy.GemMethods;
import me.realized.tm.api.TMAPI;
import net.milkbowl.vault.economy.Economy;
import net.nifheim.beelzebu.coins.CoinsAPI;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public abstract class PluginCurrency<N extends Number & Comparable<N>> {

    public static final PluginCurrency<Double> VAULT = new PluginCurrency<Double>() {
        Economy economy;

        @Override
        boolean init() {
            try {
                RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
                return registration != null && (economy = registration.getProvider()) != null;
            } catch (NoClassDefFoundError ex) {
                return false;
            }
        }

        @Override
        public Double get(Player player) {
            return isAvailable() ? economy.getBalance(player) : 0D;
        }

        @Override
        public boolean has(Player player, Double amount) {
            return isAvailable() && economy.has(player, amount);
        }

        @Override
        public void give(Player player, Double amount) {
            if (isAvailable()) economy.depositPlayer(player, amount);
        }

        @Override
        public boolean take(Player player, Double amount) {
            return isAvailable() && economy.withdrawPlayer(player, amount).transactionSuccess();
        }
    };
    public static final PluginCurrency<Integer> PLAYER_POINTS = new PluginCurrency<Integer>() {
        PlayerPointsAPI api;

        @Override
        boolean init() {
            try {
                return (api = ((PlayerPoints) Bukkit.getPluginManager().getPlugin("PlayerPoints")).getAPI()) != null;
            } catch (NoClassDefFoundError | ClassCastException ex) {
                return false;
            }
        }

        @Override
        public Integer get(Player player) {
            return isAvailable() ? api.look(player.getUniqueId()) : 0;
        }

        @Override
        public void give(Player player, Integer amount) {
            if (isAvailable()) {
                api.give(player.getUniqueId(), amount);
            }
        }

        @Override
        public boolean take(Player player, Integer amount) {
            return isAvailable() && api.take(player.getUniqueId(), amount);
        }

        @Override
        public boolean has(Player player, Integer amount) {
            return isAvailable() && api.look(player.getUniqueId()) >= amount;
        }
    };
    public static final PluginCurrency<Integer> GEMS = new PluginCurrency<Integer>() {

        GemMethods methods;

        @Override
        boolean init() {
            try {
                methods = new GemMethods();
                return true;
            } catch (NoClassDefFoundError err) {
                return false;
            }
        }

        @Override
        public Integer get(Player player) {
            return isAvailable() ? methods.getGems(player.getUniqueId()) : 0;
        }

        @Override
        public void give(Player player, Integer amount) {
            if (isAvailable()) {
                methods.addGems(player.getUniqueId(), amount);
            }
        }

        @Override
        public boolean take(Player player, Integer amount) {
            if (isAvailable() && methods.getGems(player.getUniqueId()) >= amount) {
                methods.takeGems(player.getUniqueId(), amount);
                return true;
            }
            return false;
        }

        @Override
        public boolean has(Player player, Integer amount) {
            return methods.getGems(player.getUniqueId()) >= amount;
        }
    };
    public static final PluginCurrency<Integer> TOKENS = new PluginCurrency<Integer>() {

        @Override
        boolean init() {
            try {
                Class.forName("me.realized.tm.api.TMAPI");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }

        @Override
        public Integer get(Player player) {
            return (int) TMAPI.getTokens(player); // Method returns a long, but it's an integer internally. Get your stuff together man
        }

        @Override
        public void give(Player player, Integer amount) {
            TMAPI.addTokens(player, amount);
        }

        @Override
        public boolean take(Player player, Integer amount) {
            if (TMAPI.getTokens(player) >= amount) {
                TMAPI.removeTokens(player, amount);
                return true;
            }
            return false;
        }
    };
    public static final PluginCurrency<Double> COINS = new PluginCurrency<Double>() {

        @Override
        boolean init() {
            try {
                Class.forName("net.nifheim.beelzebu.coins.CoinsAPI");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }

        @Override
        public Double get(Player player) {
            return CoinsAPI.getCoins(player.getUniqueId());
        }

        @Override
        public void give(Player player, Double amount) {
            CoinsAPI.addCoins(player.getUniqueId(), amount, false);
        }

        @Override
        public boolean take(Player player, Double amount) {
            if (CoinsAPI.getCoins(player.getUniqueId()) >= amount) {
                CoinsAPI.takeCoins(player.getUniqueId(), amount);
                return true;
            }
            return false;
        }
    };

    private boolean init, available;

    abstract boolean init();

    public final boolean isAvailable() {
        return init ? available : (init = true) & (available = init());
    }

    public abstract N get(Player player);
    public boolean has(Player player, N amount) {
        return get(player).compareTo(amount) >= 0;
    }
    public abstract void give(Player player, N amount);
    public abstract boolean take(Player player, N amount);
}
