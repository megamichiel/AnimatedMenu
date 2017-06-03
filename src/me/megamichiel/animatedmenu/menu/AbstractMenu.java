package me.megamichiel.animatedmenu.menu;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.OpenAnimation;
import me.megamichiel.animatedmenu.menu.item.ItemInfo;
import me.megamichiel.animatedmenu.util.Delay;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractMenu {

    protected final AnimatedMenuPlugin plugin;
    private final String name;
    private final MenuType menuType;
    private final MenuGrid menuGrid;

    private Function<Player, OpenAnimation> openAnimation;

    private MenuItem emptyItem;
    private boolean singleEmptyItem;

    private Delay clickDelay;

    private final Map<Player, MenuSession> sessions = new ConcurrentHashMap<>();
    private final MenuItem.SlotContext slotContext;

    protected AbstractMenu(String name, MenuType menuType) {
        this.name = name;
        this.menuType = menuType;
        menuGrid = new MenuGrid(this);
        slotContext = new MenuItem.SlotContext(plugin = AnimatedMenuPlugin.get(), menuType.getSize(), null);
    }

    public void setOpenAnimation(Function<Player, OpenAnimation> openAnimation) {
        this.openAnimation = openAnimation;
    }

    public void setClickDelay(String delayMessage, long delay) {
        clickDelay = delay <= 0 ? null : plugin.addPlayerDelay("menu_" + name, delayMessage, delay);
    }

    public boolean hasEmptyItem() {
        return emptyItem != null;
    }

    public void setEmptyItem(ItemInfo info) {
        this.emptyItem = info == null ? null : new MenuItem(this, info, 0);
    }

    public void setSingleEmptyItem(boolean singleEmptyItem) {
        this.singleEmptyItem = singleEmptyItem;
    }

    public String getName() {
        return name;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public MenuGrid getMenuGrid() {
        return menuGrid;
    }

    public AnimatedMenuPlugin getPlugin() {
        return plugin;
    }

    protected MenuSession removeViewer(Player player) {
        return sessions.remove(player);
    }

    protected void setup(Player who, Inventory inv, Consumer<? super MenuSession> action) {
        ItemStack[] contents = new ItemStack[inv.getSize()];
        BiMap<Integer, MenuItem> items = HashBiMap.create();

        BooleanSupplier animation = null;
        if (openAnimation != null) {
            OpenAnimation anim = openAnimation.apply(who);
            if (anim != null) {
                animation = () -> anim.tick(i -> inv.setItem(i, contents[i]));
            }
        }

        MenuSession session = new MenuSession(this, who, inv, items.inverse(), animation);

        action.accept(session);

        MenuItem.SlotContext ctx = new MenuItem.SlotContext(plugin, inv.getSize(), items);

        int slot;
        for (MenuItem item : menuGrid) {
            ItemInfo info = item.getInfo();
            ItemStack stack = info.load(who, session);
            if (stack == null || (slot = ctx.getSlot(who, session, info, stack)) < 0) {
                continue; // Hidden or whatever
            }
            items.forcePut(slot, item);
            contents[slot] = stack;
        }
        if (emptyItem != null) {
            ItemInfo info = emptyItem.getInfo();
            boolean single = singleEmptyItem;
            ItemStack item = single ? info.load(who, session) : null;
            for (int i = 0; i < contents.length; ++i) {
                if (contents[i] == null) {
                    contents[i] = single ? item : info.load(who, session);
                }
            }
        }

        if (animation == null) {
            inv.setContents(contents);
        }

        sessions.put(who, session);
    }

    public void click(Player player, int slot, ClickType type) {
        MenuSession session = sessions.get(player);
        if (session != null && !session.isOpening()) {
            MenuItem item = session.getItem(slot);
            if (item == null && (item = emptyItem) == null) return;

            Delay delay = clickDelay;
            if (delay == null || delay.test(player)) {
                item.getInfo().click(player, session, type);
            }
        }
    }

    public MenuSession getSession(Player player) {
        return sessions.get(player);
    }

    void itemRemoved(MenuItem item) {
        sessions.forEach((player, session) -> {
            Integer slot = session.getItems().remove(item);
            if (slot != null) {
                session.getInventory().setItem(slot, emptyItem != null ? emptyItem.getInfo().load(player, session) : null);
            }
        });
    }

    public Set<Player> getViewers() {
        return sessions.keySet();
    }

    public boolean isViewing(Player player) {
        return sessions.containsKey(player);
    }

    public void forEachSession(Consumer<? super MenuSession> action) {
        sessions.values().forEach(action);
    }

    public void forEachViewer(Consumer<? super Player> action) {
        sessions.keySet().forEach(action);
    }

    public void closeAll() {
        sessions.keySet().forEach(Player::closeInventory);
    }

    public void tick() {
        MenuItem emptyItem = this.emptyItem; // Consistency!

        boolean emptyChanged = emptyItem != null && emptyItem.tick(),
                     changed = menuGrid.tick(emptyChanged);

        if (!changed) { // Nothing's changed, only update open animations
            sessions.values().forEach(MenuSession::tick);
            return;
        }

        MenuItem.SlotContext ctx = slotContext;
        sessions.forEach((player, session) -> {
            if (session.tick()) {
                return;
            }
            BiMap<MenuItem, Integer> itemToSlot = session.getItems();
            ctx.setItems(itemToSlot.inverse());
            Inventory inv = session.getInventory();
            ItemInfo info;
            ItemStack stack, newStack;
            int slot;
            for (MenuItem item : menuGrid) {
                info = item.getInfo();
                Integer old = itemToSlot.get(item);
                if (old != null && (stack = inv.getItem(slot = old)) != null) { // Item exists in the inventory
                    try {
                        newStack = item.canRefresh() ? info.apply(player, session, stack) : stack;
                    } catch (Exception ex) {
                        plugin.nag("Failed to tick item " + info + " in menu " + name + "!");
                        ex.printStackTrace();
                        newStack = stack;
                    }
                    if (newStack == null) {
                        inv.setItem(slot, null);
                        itemToSlot.remove(item);
                    } else if (item.canSlotChange()) {
                        if (slot != (slot = ctx.getSlot(player, session, info, stack))) {
                            inv.setItem(old, null);
                            if (slot == -1) {
                                itemToSlot.remove(item);
                            } else  {
                                itemToSlot.forcePut(item, slot);
                                inv.setItem(slot, newStack);
                            }
                        } else if (newStack != stack) {
                            inv.setItem(slot, newStack);
                        }
                    } else {
                        if (newStack != stack) inv.setItem(slot, newStack);
                        ctx.addItem(player, session, slot, info, newStack.getAmount());
                    }
                } else {
                    try {
                        stack = info.load(player, session);
                    } catch (Exception ex) {
                        plugin.nag("Failed to load item " + info + " in menu " + name + "!");
                        ex.printStackTrace();
                        continue;
                    }
                    if (stack != null && (slot = ctx.getSlot(player, session, info, stack)) != -1) {
                        inv.setItem(slot, stack);
                        itemToSlot.forcePut(item, slot);
                    }
                }
            }
            if (emptyItem != null) {
                info = emptyItem.getInfo();
                BiMap<Integer, MenuItem> slotToItem = itemToSlot.inverse();
                for (int i = inv.getSize(); --i >= 0;) {
                    if (!slotToItem.containsKey(i)) {
                        if ((stack = inv.getItem(i)) != null) {
                            try {
                                if (emptyChanged && stack != info.apply(player, session, stack)) {
                                    inv.setItem(i, stack);
                                }
                            } catch (Exception ex) {
                                plugin.nag("Failed to update empty item in menu " + name + "!");
                                ex.printStackTrace();
                            }
                        } else {
                            try {
                                inv.setItem(i, info.load(player, session));
                            } catch (Exception ex) {
                                plugin.nag("Failed to load empty item in menu " + name + "!");
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
            ctx.reset();
        });
        menuGrid.forEach(MenuItem::postTick);
        if (emptyItem != null) emptyItem.postTick();
    }
}
