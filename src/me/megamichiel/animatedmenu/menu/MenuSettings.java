package me.megamichiel.animatedmenu.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class MenuSettings {

	private ItemStack opener;
	private boolean openerName, openerLore;
	private boolean openOnJoin;
	private int openerJoinSlot;
	private String[] openCommands;
    private boolean hiddenFromCommand = false;

    private final List<Consumer<? super Player>> openListeners = new ArrayList<>();
    private final List<Consumer<? super Player>> closeListeners = new ArrayList<>();
	
	public boolean shouldOpenOnJoin()
	{
		return openOnJoin;
	}
	
	public boolean hasOpenerName() {
		return openerName;
	}
	
	public boolean hasOpenerLore() {
		return openerLore;
	}
	
	public void setOpener(ItemStack opener) {
		this.opener = opener;
		ItemMeta meta = opener.getItemMeta();
		openerName = meta.hasDisplayName();
		openerLore = meta.hasLore();
	}

    public ItemStack getOpener() {
        return opener;
    }

    public void setOpenOnJoin(boolean openOnJoin) {
        this.openOnJoin = openOnJoin;
    }

    public void setOpenerJoinSlot(int openerJoinSlot) {
        this.openerJoinSlot = openerJoinSlot;
    }

    public int getOpenerJoinSlot() {
        return openerJoinSlot;
    }

    public void addOpenListener(Consumer<? super Player> listener) {
        openListeners.add(listener);
    }

    public void addCloseListener(Consumer<? super Player> listener) {
        closeListeners.add(listener);
    }

    public List<Consumer<? super Player>> getOpenListeners() {
        return Collections.unmodifiableList(openListeners);
    }

    public List<Consumer<? super Player>> getCloseListeners() {
        return Collections.unmodifiableList(closeListeners);
    }

    public void setOpenCommands(String[] openCommands) {
        this.openCommands = openCommands;
    }

    public String[] getOpenCommands() {
        return openCommands;
    }

    public void setHiddenFromCommand(boolean hiddenFromCommand) {
        this.hiddenFromCommand = hiddenFromCommand;
    }

    public boolean isHiddenFromCommand() {
        return hiddenFromCommand;
    }
}
