package me.megamichiel.animatedmenu.util;

import me.megamichiel.animationlib.util.ReflectClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class InventoryTitleUpdater {

    private final ReflectClass.Method getHandle, sendPacket, updateInventory;
    private final ReflectClass.Field activeContainer, playerConnection, windowId;
    private final ReflectClass.Constructor chatMessage, openWindow;

    public InventoryTitleUpdater() throws ReflectClass.ReflectException {
        ReflectClass.Package nms = ReflectClass.Package.NMS,
                             cbk = ReflectClass.Package.CBK;

        getHandle = cbk.getClass("entity.CraftPlayer").getMethod("getHandle");
        activeContainer = nms.getClass("EntityHuman").getField("activeContainer");
        windowId = activeContainer.getReflectType().getField("windowId");
        chatMessage = nms.getClass("ChatComponentText").getConstructor(String.class);
        playerConnection = getHandle.getReflectType().getField("playerConnection");
        openWindow = nms.getClass("PacketPlayOutOpenWindow").getConstructor(
                int.class, String.class, nms.getClass("IChatBaseComponent").getHandle(), int.class, int.class
        );
        sendPacket = playerConnection.getReflectType().getMethod("sendPacket", openWindow.getOwner().getInterfaces()[0]);
        updateInventory = getHandle.getReflectType().getMethod("updateInventory", activeContainer.getType());
    }

    public void update(Player player, Inventory inventory, String type, String title) throws ReflectClass.ReflectException {
        Object handle = getHandle.invoke(player),
            container = activeContainer.get(handle);
        sendPacket.invoke(playerConnection.get(handle), openWindow.newInstance(
                windowId.get(container), type, title == null ? "" : chatMessage.newInstance(title), inventory.getSize(), 0
        ));
        updateInventory.invoke(handle, container);
    }
}
