package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animationlib.command.CommandInfo;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MenuSettings {

    private final AnimatedMenu menu;

    private ItemStack opener;
    private boolean openerName, openerLore;
    private int openerJoinSlot = -1;

    private boolean openOnJoin;
    private CommandInfo openCommand;
    private boolean hiddenFromCommand = false, lenientArgs = true, saveNavigation = false;

    private IPlaceholder<String> permission, permissionMessage;

    private final List<Predicate<? super Player>> awaits = new ArrayList<>();
    private IPlaceholder<String> waitMessage;

    private final List<BiConsumer<? super Player, ? super MenuSession>> openListeners = new ArrayList<>();
    private final List<BiConsumer<? super Player, ? super MenuSession>> closeListeners = new ArrayList<>();

    MenuSettings(AnimatedMenu menu) {
        this.menu = menu;
    }
    
    public boolean shouldOpenOnJoin() {
        return openOnJoin;
    }
    
    public void setOpener(ItemStack opener, int slot) {
        this.opener = opener;
        this.openerJoinSlot = slot;
        if (opener == null || !opener.hasItemMeta()) return;
        ItemMeta meta = opener.getItemMeta();
        openerName = meta.hasDisplayName();
        openerLore = meta.hasLore();
    }

    public boolean hasOpener() {
        return opener != null;
    }

    public boolean giveOpener(PlayerInventory inv, boolean add) {
        if (opener != null) {
            if (add) inv.addItem(opener);
            else if (openerJoinSlot != -1) inv.setItem(openerJoinSlot, opener);
            return true;
        }
        return false;
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

    public void setOpenOnJoin(boolean openOnJoin) {
        this.openOnJoin = openOnJoin;
    }

    public void addListener(BiConsumer<? super Player, ? super MenuSession> listener, boolean close) {
        (close ? closeListeners : openListeners).add(listener);
    }

    public void addListener(Consumer<? super Player> listener, boolean close) {
        addListener((player, session) -> listener.accept(player), close);
    }

    void callListeners(Player player, MenuSession session, boolean close) {
        for (BiConsumer<? super Player, ? super MenuSession> listener : (close ? closeListeners : openListeners))
            listener.accept(player, session);
    }

    public void setOpenCommand(String name, String usage, String description, String[] aliases) {
        if (name == null) openCommand = null;
        else openCommand = new CommandInfo(name, menu).usage(usage).desc(description).aliases(aliases);
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

    public void setLenientArgs(boolean lenientArgs) {
        this.lenientArgs = lenientArgs;
    }

    public boolean hasLenientArgs() {
        return lenientArgs;
    }

    public void setSaveNavigation(boolean saveNavigation) {
        this.saveNavigation = saveNavigation;
    }

    public boolean saveNavigation() {
        return saveNavigation;
    }

    public void setPermission(IPlaceholder<String> permission) {
        this.permission = permission;
    }

    public IPlaceholder<String> getPermission() {
        return permission;
    }

    public void setPermissionMessage(IPlaceholder<String> permissionMessage) {
        this.permissionMessage = permissionMessage;
    }

    public IPlaceholder<String> getPermissionMessage() {
        return permissionMessage;
    }

    public List<Predicate<? super Player>> getAwaits() {
        return awaits;
    }

    public void setWaitMessage(IPlaceholder<String> waitMessage) {
        this.waitMessage = waitMessage;
    }

    public IPlaceholder<String> getWaitMessage() {
        return waitMessage;
    }

    public AnimatedMenu getMenu() {
        return menu;
    }
}
