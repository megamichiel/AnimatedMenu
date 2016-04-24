package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.command.SoundCommand;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuSettings {

	private ItemStack opener;
	private boolean openerName, openerLore;
	private boolean openOnJoin;
	private int openerJoinSlot;
    private SoundCommand.SoundInfo openSound;
	private String[] openCommands;
    private boolean hiddenFromCommand = false;
	
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

    public void setOpenSound(SoundCommand.SoundInfo openSound) {
        this.openSound = openSound;
    }

    public SoundCommand.SoundInfo getOpenSound() {
        return openSound;
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
