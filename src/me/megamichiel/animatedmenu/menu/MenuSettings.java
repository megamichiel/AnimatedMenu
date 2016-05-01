package me.megamichiel.animatedmenu.menu;

import com.google.common.base.Predicate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuSettings {

	private ItemStack opener;
	private boolean openerName, openerLore;
	private boolean openOnJoin;
	private int openerJoinSlot;
	private String[] openCommands;
    private boolean hiddenFromCommand = false;

    private final List<Predicate<? super Player>> openListeners = new ArrayList<>();
	
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

    public void addOpenListener(Predicate<? super Player> listener) {
        openListeners.add(listener);
    }

    public List<Predicate<? super Player>> getOpenListeners() {
        return Collections.unmodifiableList(openListeners);
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
