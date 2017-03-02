package me.megamichiel.animatedmenu.menu;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedOpenAnimation;
import me.megamichiel.animatedmenu.animation.OpenAnimation;
import me.megamichiel.animatedmenu.util.Delay;
import me.megamichiel.animationlib.config.AbstractConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class AbstractMenu {

    protected final AnimatedMenuPlugin plugin;
    private final String name;
    final MenuType menuType;
    protected final MenuLoader loader;
    protected final MenuGrid menuGrid;

    private final AnimatedOpenAnimation openAnimation;

    private MenuItem emptyItem;

    private int slotUpdateDelay, slotUpdateTimer;
    private Delay clickDelay;

    private final Map<Player, Session> sessions = new ConcurrentHashMap<>();
    private final ItemInfo.SlotContext slotContext;

    protected AbstractMenu(AnimatedMenuPlugin plugin, String name,
                           MenuType menuType, MenuLoader loader) {
        this.plugin = plugin;
        this.name = name;
        this.menuType = menuType;
        this.loader = loader;
        menuGrid = new MenuGrid(this);
        openAnimation = new AnimatedOpenAnimation(menuType);
        slotContext = new ItemInfo.SlotContext(plugin, new ItemStack[menuType.getSize()]);
    }

    public void dankLoad(AbstractConfig config) {
        setSlotUpdateDelay(config.getInt("slot-update-delay", 20));

        double speed = config.getDouble("animation-speed", 1);
        openAnimation.init(plugin, speed <= 0 ? 1 : speed);
        openAnimation.load(plugin, config, "open-animation");

        AbstractConfig section = config.getSection("empty-item");
        if (section != null) {
            plugin.getMenuRegistry().getMenuLoader().loadItem(this, "empty-item", section, false, info -> emptyItem = new MenuItem(info, 0));
        }
    }

    public void setClickDelay(String delayMessage, long delay) {
        clickDelay = delay <= 0 ? null : plugin.addPlayerDelay(delayMessage, delay);
    }

    public void setSlotUpdateDelay(int slotUpdateDelay) {
        this.slotUpdateDelay = Math.max(slotUpdateDelay, 0);
    }

    boolean hasEmptyItem() {
        return emptyItem != null;
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

    protected boolean removeViewer(Player player) {
        if (sessions.remove(player) != null) {
            menuGrid.forEach(item -> item.getInfo().menuClosed(player));
            return true;
        }
        return false;
    }

    protected void setup(Player who, Inventory inv) {
        ItemStack[] contents = new ItemStack[inv.getSize()];
        BiMap<MenuItem, Integer> items = HashBiMap.create();

        ItemInfo.SlotContext ctx = new ItemInfo.SlotContext(plugin, contents);
        ctx.setItems(items);

        int slot;
        for (MenuItem item : menuGrid) {
            ItemInfo info = item.getInfo();
            ItemStack stack = info.load(who);
            if (stack == null) continue; // Hidden or whatever
            items.forcePut(item, slot = ctx.getSlot(who, info, stack));
            contents[slot] = stack;
        }
        if (emptyItem != null) {
            ItemInfo info = emptyItem.getInfo();
            for (int i = 0; i < contents.length; i++)
                if (contents[i] == null) contents[i] = info.load(who);
        }

        OpenAnimation anim = openAnimation.next();
        OpenAnimation.Animation opening;

        if (anim != null) {
            opening = anim.newAnimation(update -> {
                for (int i : update)
                    if (i >= 0 && contents[i] != null)
                        inv.setItem(i, contents[i]);
            });
        } else {
            inv.setContents(contents);
            opening = null;
        }

        sessions.put(who, new Session(inv, items, opening));
    }

    public void click(Player player, int slot, ClickType type) {
        Session s = sessions.get(player);
        if (s != null && s.opening == null) {
            MenuItem item = s.items.inverse().get(slot);
            if (item == null) {
                if ((item = emptyItem) == null) return;
            } else if (item.isRemoved()) {
                s.items.inverse().remove(slot);
                return;
            }
            Delay delay = clickDelay;
            if (delay == null || delay.test(player)) {
                item.getInfo().click(player, type);
            }
        }
    }

    void itemRemoved(MenuItem item) {
        sessions.forEach((player, session) -> {
            Integer slot = session.items.remove(item);
            if (slot != null) {
                session.inventory.setItem(slot, emptyItem != null ? emptyItem.getInfo().load(player) : null);
            }
        });
    }

    public Set<Player> getViewers() {
        return sessions.keySet();
    }

    public boolean isViewing(Player player) {
        return sessions.containsKey(player);
    }

    public void forEachViewer(Consumer<? super Player> action) {
        sessions.keySet().forEach(action);
    }

    public void closeAll() {
        sessions.keySet().forEach(Player::closeInventory);
    }

    public void requestSlotUpdate() {
        slotUpdateTimer = 0;
    }

    public void tick() {
        MenuItem emptyItem = this.emptyItem; // Consistency!

        boolean emptyChanged = emptyItem != null && emptyItem.tick(),
                changed = emptyChanged;
        for (MenuItem item : menuGrid)
            changed |= item.tick();
        boolean updateSlots = menuGrid.hasDynamicSlots() && slotUpdateTimer-- == 0;
        if (updateSlots) slotUpdateTimer = slotUpdateDelay;
        else if (!changed) { // Nothing's changed, only update open animations
            for (Session session : sessions.values())
                if (session.opening != null && session.opening.tick())
                    session.opening = null;
            return;
        }

        ItemInfo.SlotContext ctx = slotContext;
        ItemStack[] contents = ctx.getContents();
        double[] weights = ctx.getWeights();
        sessions.forEach((player, session) -> {
            if (session.opening != null) {
                if (session.opening.tick()) session.opening = null;
                else return;
            }
            BiMap<MenuItem, Integer> itemToSlot = session.items;
            ctx.setItems(itemToSlot);
            Inventory inv = session.inventory;
            ItemInfo info;
            ItemStack stack;
            int slot;
            for (MenuItem item : menuGrid) {
                info = item.getInfo();
                Integer old = itemToSlot.get(item);
                if (old == null) {
                    if ((stack = info.load(player)) != null) {
                        itemToSlot.forcePut(item, slot = ctx.getSlot(player, info, stack));
                        contents[slot] = stack;
                    }
                } else {
                    if ((stack = inv.getItem(slot = old)) != null) {
                        if (updateSlots && slot != (slot = ctx.getSlot(player, info, stack))) {
                            itemToSlot.forcePut(item, slot);
                            contents[slot] = info.apply(player, stack);
                            inv.setItem(slot = old, null);
                            contents[slot] = null;
                        } else contents[slot] = info.apply(player, stack);
                    }
                }
            }
            info = emptyItem != null ? emptyItem.getInfo() : null;
            for (int i = contents.length; i-- > 0;) {
                if ((stack = contents[i]) != null) {
                    inv.setItem(i, stack);
                    contents[i] = null;
                    weights[i] = 0D;
                } else if (info != null) {
                    if ((stack = inv.getItem(i)) != null) {
                        if (emptyChanged && stack != info.apply(player, stack))
                            inv.setItem(i, stack);
                    } else inv.setItem(i, info.load(player));
                }
            }
        });
        menuGrid.forEach(MenuItem::postTick);
        if (emptyItem != null) emptyItem.postTick();
    }

    private static class Session {

        private final Inventory inventory;
        private final BiMap<MenuItem, Integer> items;

        private OpenAnimation.Animation opening;

        private Session(Inventory inventory, BiMap<MenuItem, Integer> items, OpenAnimation.Animation opening) {
            this.inventory = inventory;
            this.items = items;
            this.opening = opening;
        }
    }
}
