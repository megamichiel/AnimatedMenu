package me.megamichiel.animatedmenu.util;

import me.megamichiel.animationlib.util.ReflectClass;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class InventoryTitleUpdater {

    private final ReflectClass.Method getInventory, getContainerName, getHandle, sendPacket, updateInventory;
    private final ReflectClass.Field activeContainer, playerConnection, windowId;
    private final ReflectClass.Constructor chatMessage, openWindow;

    public InventoryTitleUpdater() throws ReflectClass.ReflectException {
        ReflectClass.Package nms = ReflectClass.Package.NMS;

        getHandle = ReflectClass.Package.CBK.getClass("entity.CraftPlayer").getMethod("getHandle");
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
    }

    private final Map<InventoryType, String> nmsNames = new HashMap<>();

    public void update(Player player, Inventory inventory, String title) {
        String nmsName = nmsNames.computeIfAbsent(inventory.getType(), type -> {
            Object inventoryHandle = getInventory.invoke(inventory);
            if (getContainerName.canInvoke(inventoryHandle)) {
                return getContainerName.invokeGeneric(inventoryHandle);
            }
            return "minecraft:container";
        });
        Object handle = getHandle.invoke(player);
        Object container = activeContainer.get(handle);
        sendPacket.invoke(playerConnection.get(handle), openWindow.newInstance(
                windowId.get(container), nmsName,
                title == null ? "" : chatMessage.newInstance(title),
                inventory.getSize(), 0
        ));
        updateInventory.invokeGeneric(handle, container);
    }
}
