package me.megamichiel.animatedmenu;

import java.util.List;

import lombok.Getter;
import me.megamichiel.animatedmenu.menu.AnimatedMenu;
import me.megamichiel.animatedmenu.menu.MenuItem;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;

public class MenuPacketListener implements PacketListener {
	
	public static void init(AnimatedMenuPlugin plugin) {
		ProtocolLibrary.getProtocolManager().addPacketListener(new MenuPacketListener(plugin));
	}
	
	@Getter
	private final ListeningWhitelist sendingWhitelist = ListeningWhitelist.newBuilder()
			.types(PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT).build();
	@Getter
	private final AnimatedMenuPlugin plugin;
	
	public MenuPacketListener(AnimatedMenuPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public ListeningWhitelist getReceivingWhitelist() {
		return ListeningWhitelist.EMPTY_WHITELIST;
	}
	
	@Override
	public void onPacketReceiving(PacketEvent event) {}
	
	@Override
	public void onPacketSending(PacketEvent event) {
		AnimatedMenu menu = plugin.getMenuRegistry().getOpenedMenu(event.getPlayer());
		if(menu == null) return;
		Player p = event.getPlayer();
		PacketContainer packet = event.getPacket();
		int size = menu.getType().getSize();
		if(packet.getType() == PacketType.Play.Server.WINDOW_ITEMS) {
			ItemStack[] items = packet.getItemArrayModifier().read(0);
			for(int i=0;i<items.length;i++) {
				if(items[i] == null || i >= size) continue;
				MenuItem menuItem = menu.getMenuGrid().getItem(i);
				if(menuItem.getSettings().getPermission() != null
						&& !p.hasPermission(menuItem.getSettings().getPermission())
						&& menuItem.getSettings().isHiddenWithoutPermission()) {
					items[i] = null;
					continue;
				}
				ItemMeta meta = items[i].getItemMeta();
				meta.setDisplayName(plugin.applyPlayerPlaceHolders(p, meta.getDisplayName()));
				List<String> lore = meta.getLore();
				if(lore != null) {
					lore = plugin.applyPlayerPlaceHolders(p, lore);
					meta.setLore(lore);
				}
				items[i].setItemMeta(meta);
			}
			packet.getItemArrayModifier().write(0, items);
		} else {
			int slot = packet.getIntegers().read(1);
			if(slot < 0 || slot >= size || menu.getMenuGrid().getItem(slot) == null) return;
			MenuItem menuItem = menu.getMenuGrid().getItem(slot);
			if(menuItem.getSettings().getPermission() != null
					&& !p.hasPermission(menuItem.getSettings().getPermission())
					&& menuItem.getSettings().isHiddenWithoutPermission()) {
				packet.getItemModifier().write(0, null);
				return;
			}
			ItemStack item = packet.getItemModifier().read(0);
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(plugin.applyPlayerPlaceHolders(p, meta.getDisplayName()));
			List<String> lore = meta.getLore();
			if(lore != null) {
				lore = plugin.applyPlayerPlaceHolders(p, lore);
				meta.setLore(lore);
			}
			item.setItemMeta(meta);
		}
	}
}
