package me.megamichiel.animatedmenu.util;

import me.megamichiel.animationlib.util.ReflectClass;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class InventoryTitleUpdater {

    private ReflectClass.Method getInventory, getContainerName, getHandle, sendPacket, updateInventory;
    private ReflectClass.Field activeContainer, playerConnection, windowId;
    private ReflectClass.Constructor chatMessage, openWindow;

    private boolean init(Player player) {
        if (getInventory != null) return true;

        try {
            ReflectClass.Package nms = ReflectClass.Package.NMS;

            getHandle = new ReflectClass(player.getClass()).getMethod("getHandle");
            activeContainer = nms.getClass("EntityHuman").getField("activeContainer");
            windowId = activeContainer.getReflectType().getField("windowId");
            getContainerName = nms.getClass("ITileEntityContainer").getMethod("getContainerName");
            getInventory = ReflectClass.Package.CBK.getClass("inventory.CraftInventory").getMethod("getInventory");
            chatMessage = nms.getClass("ChatComponentText").getConstructor(String.class);
            playerConnection = getHandle.getReflectType().getField("playerConnection");
            openWindow = nms.getClass("PacketPlayOutOpenWindow").getConstructor(
                    int.class, String.class, nms.getClass("IChatBaseComponent").getHandle(), int.class, int.class
            );
            sendPacket = playerConnection.getReflectType().getMethod("sendPacket", openWindow.getOwner().getInterfaces()[0]);
            updateInventory = getHandle.getReflectType().getMethod("updateInventory", activeContainer.getType());

            return true;
        } catch (ReflectClass.ReflectException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private final Map<InventoryType, String> nmsNames = new HashMap<>();

    public void update(Player player, Inventory inventory, String title) {
        String nmsName = nmsNames.computeIfAbsent(inventory.getType(), type -> {
            if (init(player)) {
                Object inventoryHandle = getInventory.invoke(inventory);
                if (getContainerName.getOwner().isInstance(inventoryHandle)) {
                    return (String) getContainerName.invoke(inventoryHandle);
                }
            }
            return "minecraft:container";
        });
        Object handle = getHandle.invoke(player);
        Object container = activeContainer.get(handle);
        sendPacket.invoke(playerConnection.get(handle), openWindow.newInstance(
                windowId.get(container), nmsName,
                chatMessage.newInstance(title),
                inventory.getSize(), 0
        ));
        updateInventory.invoke(handle, container);
    }
}
