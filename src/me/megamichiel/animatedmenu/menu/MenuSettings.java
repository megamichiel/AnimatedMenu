package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animationlib.command.CommandInfo;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MenuSettings {

    private final Menu menu;

    private ItemStack opener;
    private boolean openerName, openerLore;
    private int openerJoinSlot = -1;

    private boolean openOnJoin;
    private CommandInfo openCommand;
    private String fallbackCommand;
    private boolean hiddenFromCommand = false, saveNavigation = false;

    private IPlaceholder<String> permission, permissionMessage;

    private final List<Predicate<? super Player>> awaits = new ArrayList<>();
    private IPlaceholder<String> waitMessage;

    private final List<BiConsumer<? super Player, ? super MenuSession>> openListeners = new ArrayList<>();
    private final List<BiConsumer<? super Player, ? super MenuSession>> closeListeners = new ArrayList<>();

    MenuSettings(Menu menu) {
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
            if (add) {
                inv.addItem(opener);
            } else if (openerJoinSlot >= 0) {
                inv.setItem(openerJoinSlot, opener);
            }
            return true;
        }
        return false;
    }

    public boolean canOpenWith(ItemStack item, ItemMeta meta) {
        ItemStack opener = this.opener;
        if (opener == null || item.getType() != opener.getType() || item.getAmount() < opener.getAmount()
                || (item.getDurability() != Short.MAX_VALUE && item.getDurability() != opener.getDurability())) {
            return false;
        }
        if (openerName || openerLore) {
            ItemMeta im = opener.getItemMeta();
            return (!openerName || im.getDisplayName().equals(meta.getDisplayName())) &&
                   (!openerLore || im.getLore().equals(meta.getLore()));
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

    public String getFallbackCommand() {
        return fallbackCommand;
    }

    public void setFallbackCommand(String fallbackCommand) {
        this.fallbackCommand = fallbackCommand;
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

    public Menu getMenu() {
        return menu;
    }
}
