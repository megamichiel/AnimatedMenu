package me.megamichiel.animatedmenu.util;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class InventoryTitleUpdater {

    private Method getInventory, getContainerName, getHandle, sendPacket, updateInventory;
    private Field activeContainer, playerConnection, windowId;
    private Constructor<?> chatMessage, openWindow;

    private boolean init(Player player) {
        if (getInventory != null) return true;
        try {
            getHandle = player.getClass().getMethod("getHandle");

            String cbkPackage = (cbkPackage = getHandle.getDeclaringClass().getPackage().getName()).substring(0, cbkPackage.lastIndexOf('.')),
                    nmsPackage = getHandle.getReturnType().getPackage().getName();

            activeContainer = getHandle.getReturnType().getSuperclass().getDeclaredField("activeContainer");
            windowId = activeContainer.getType().getDeclaredField("windowId");
            getContainerName = Class.forName(nmsPackage + ".ITileEntityContainer").getDeclaredMethod("getContainerName");
            getInventory = Class.forName(cbkPackage + ".inventory.CraftInventory").getDeclaredMethod("getInventory");

            chatMessage = Class.forName(nmsPackage + ".ChatComponentText").getConstructor(String.class);

            playerConnection = getHandle.getReturnType().getDeclaredField("playerConnection");
            openWindow = Class.forName(nmsPackage + ".PacketPlayOutOpenWindow").getDeclaredConstructor(
                    int.class, String.class, Class.forName(nmsPackage + ".IChatBaseComponent"), int.class, int.class
            );
            sendPacket = playerConnection.getType().getDeclaredMethod("sendPacket", openWindow.getDeclaringClass().getInterfaces()[0]);

            updateInventory = getHandle.getReturnType().getDeclaredMethod("updateInventory", activeContainer.getType());

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private final Map<InventoryType, String> nmsNames = new HashMap<>();

    public void update(Player player, Inventory inventory, String title) {
        try {
            String nmsName = nmsNames.computeIfAbsent(inventory.getType(), type -> {
                if (init(player)) {
                    try {
                        Object inventoryHandle = getInventory.invoke(inventory);
                        if (getContainerName.getDeclaringClass().isInstance(inventoryHandle)) {
                            return (String) getContainerName.invoke(inventoryHandle);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
