package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animationlib.command.CommandInfo;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MenuSettings {

    private final AnimatedMenu menu;

    private ItemStack opener;
    private boolean openerName, openerLore;

    private boolean openOnJoin;
    private int openerJoinSlot = -1;
    private CommandInfo openCommand;
    private boolean hiddenFromCommand = false;

    private final List<Consumer<? super Player>> openListeners = new ArrayList<>();
    private final List<Consumer<? super Player>> closeListeners = new ArrayList<>();

    MenuSettings(AnimatedMenu menu) {
        this.menu = menu;
    }
    
    public boolean shouldOpenOnJoin() {
        return openOnJoin;
    }
    
    void setOpener(ItemStack opener, int slot) {
        this.opener = opener;
        this.openerJoinSlot = slot;
        if (opener == null || !opener.hasItemMeta()) return;
        ItemMeta meta = opener.getItemMeta();
        openerName = meta.hasDisplayName();
        openerLore = meta.hasLore();
    }

    public void giveOpener(PlayerInventory inv, boolean add) {
        if (opener != null) {
            if (add) inv.addItem(opener);
            else if (openerJoinSlot != -1) inv.setItem(openerJoinSlot, opener);
        }
    }

    public boolean canOpenWith(MaterialData data, int amount, ItemMeta meta) {
        if (opener == null) return false;
        if (!data.equals(opener.getData()) || amount < opener.getAmount())
            return false;
        if (openerName || openerLore) {
            ItemMeta im = opener.getItemMeta();
            if (openerName && !im.getDisplayName().equals(meta.getDisplayName()))
                return false;
            if (openerLore && !im.getLore().equals(meta.getLore()))
                return false;
        }
        return true;
    }

    void setOpenOnJoin(boolean openOnJoin) {
        this.openOnJoin = openOnJoin;
    }

    public void addListener(Consumer<? super Player> listener, boolean close) {
        (close ? closeListeners : openListeners).add(listener);
    }

    void callListeners(Player player, boolean close) {
        for (Consumer<? super Player> listener : (close ? closeListeners : openListeners))
            listener.accept(player);
    }

    void setOpenCommand(String name, String usage, String description, String[] aliases) {
        if (name == null) openCommand = null;
        else openCommand = new CommandInfo(name, menu)
                .usage(usage).desc(description).aliases(aliases);
    }

    public CommandInfo getOpenCommand() {
        return openCommand;
    }

    public void setHiddenFromCommand(boolean hiddenFromCommand) {
        this.hiddenFromCommand = hiddenFromCommand;
    }

    public boolean isHiddenFromCommand() {
        return hiddenFromCommand;
    }

    public AnimatedMenu getMenu() {
        return menu;
    }
}
