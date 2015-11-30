package me.megamichiel.animatedmenu.menu;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

@ToString
public class MenuItem {
	
	@Getter @Setter
	private ItemStack handle;
	@Getter
	private final MenuItemSettings settings;
	private final AtomicInteger currentTick = new AtomicInteger();
	
	public MenuItem(MenuItemSettings settings) {
		this.settings = settings;
		handle = load(settings);
	}
	
	public void tick() {
		if(currentTick.getAndIncrement() == settings.getFrameDelay()) {
			currentTick.set(0);
			ItemStack material = settings.getMaterial().next();
			handle.setType(material.getType());
			handle.setAmount(material.getAmount());
			handle.setDurability(material.getDurability());
			
			ItemMeta meta = handle.getItemMeta();
			meta.setDisplayName(settings.getDisplayName().next().toString());
			meta.setLore(settings.getLore().next().toStringList());
			if(meta instanceof LeatherArmorMeta) {
				((LeatherArmorMeta) meta).setColor(settings.getLeatherArmorColor());
			} else if(meta instanceof SkullMeta) {
				((SkullMeta) meta).setOwner(settings.getSkullOwner());
			}
			handle.setItemMeta(meta);
		}
	}
	
	private static ItemStack load(MenuItemSettings settings) {
		ItemStack item = settings.getMaterial().next().clone();
		item.addUnsafeEnchantments(settings.getEnchantments());
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(settings.getDisplayName().next().toString());
		meta.setLore(settings.getLore().next().toStringList());
		item.setItemMeta(meta);
		return item;
	}
}
