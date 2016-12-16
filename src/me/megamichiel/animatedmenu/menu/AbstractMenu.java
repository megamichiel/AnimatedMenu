package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedOpenAnimation;
import me.megamichiel.animatedmenu.animation.OpenAnimation;
import me.megamichiel.animationlib.config.AbstractConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractMenu {

    protected final AnimatedMenuPlugin plugin;
    private final String name;
    final MenuType menuType;
    protected final MenuLoader loader;
    protected final MenuGrid menuGrid;

    private final AnimatedOpenAnimation openAnimation;

    private boolean dynamicSlots;
    private MenuItem emptyItem;

    private int slotUpdateDelay, slotUpdateTimer;

    private final Map<Player, Session> sessions = new ConcurrentHashMap<>();

    protected AbstractMenu(AnimatedMenuPlugin plugin, String name,
                           MenuType menuType, MenuLoader loader) {
        this.plugin = plugin;
        this.name = name;
        this.menuType = menuType;
        this.loader = loader;
        menuGrid = new MenuGrid(this, menuType.getSize());
        openAnimation = new AnimatedOpenAnimation(menuType);
    }

    public void dankLoad(AbstractConfig config) {
        int delay = config.getInt("slot-update-delay", 20);
        if (delay < 0) delay = 0;
        slotUpdateDelay = delay;

        double speed = config.getDouble("animation-speed", 1);
        openAnimation.init(plugin, speed <= 0 ? 1 : speed);
        openAnimation.load(plugin, config, "open-animation");

        AbstractConfig section = config.getSection("empty-item");
        if (section != null) {
            MenuItemInfo info = plugin.getMenuRegistry()
                    .getMenuLoader().loadItem(this, "empty-item", section);
            if (info != null) emptyItem = new MenuItem(info);
        }
    }

    public void init() {
        dynamicSlots = false;
        for (int i = 0; i < menuGrid.getSize(); i++)
            if (menuGrid.getItems()[i].getInfo().hasDynamicSlot()) {
                dynamicSlots = true;
                break;
            }
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
        return sessions.remove(player) != null;
    }

    protected void setup(Player who, Inventory inv) {
        ItemStack[] contents = new ItemStack[inv.getSize()];
        MenuItem[] items = new MenuItem[contents.length];
        int[] slots = new int[menuType.getSize()];
        for (int slot = 0, result; slot < menuGrid.getSize(); slot++) {
            MenuItem item = menuGrid.getItems()[slot];
            MenuItemInfo info = item.getInfo();
            if (!info.isHidden(who)) {
                ItemStack stack = info.load(who);
                items[result = slots[slot] = info.getSlot(who, contents, stack)] = item;
                contents[result] = stack;
            }
        }
        if (emptyItem != null) {
            MenuItemInfo info = emptyItem.getInfo();
            for (int slot = 0; slot < items.length; slot++) {
                if (items[slot] == null) {
                    items[slot] = emptyItem;
                    contents[slot] = info.load(who);
                }
            }
        }
        OpenAnimation anim = openAnimation.next();
        OpenAnimation.Animation opening;

        if (anim != null) opening = anim.newAnimation(update -> {
            for (int slot : update) if (slot != -1 && contents[slot] != null)
                inv.setItem(slot, contents[slot]);
        });
        else {
            inv.setContents(contents);
            opening = null;
        }
        sessions.put(who, new Session(inv, items, slots, opening));
    }

    MenuItem getItem(Player who, int slot) {
        Session s = sessions.get(who);
        return s == null || s.opening != null ? null : s.items[slot];
    }

    Set<Player> getViewers() {
        return sessions.keySet();
    }

    public void tick() {
        if (plugin == null) return;
        MenuItem[] items = menuGrid.getItems();
        int size = menuGrid.getSize();
        if (dynamicSlots) {
            boolean updateSlots = slotUpdateTimer-- == 0;
            if (updateSlots) slotUpdateTimer = slotUpdateDelay;
            boolean changed = updateSlots;
            for (int i = 0; i < size; i++) changed |= items[i].tick();
            if (changed) {
                for (Session session : sessions.values())
                    Arrays.fill(session.items, null);
                ItemStack[] contents = new ItemStack[items.length];
                sessions.forEach((who, session) -> {
                    if (session.opening != null) {
                        if (session.opening.tick()) session.opening = null;
                        else return;
                    }
                    MenuItem[] visible = session.items;
                    int[] slots = session.slots;
                    MenuItem item;
                    for (int i = 0; i < size; i++) {
                        if (!(item = items[i]).getInfo().isHidden(who)) {
                            ItemStack stack = item.getInfo().load(who);
                            int slot;
                            if (updateSlots) slot = item.getInfo().getSlot(who, contents, stack);
                            else slot = slots[i];
                            visible[slot] = item;
                            contents[slot] = stack;
                        }
                    }
                    if (emptyItem != null) {
                        MenuItemInfo info = emptyItem.getInfo();
                        for (int slot = 0; slot < items.length; slot++) {
                            if (items[slot] == null) {
                                items[slot] = emptyItem;
                                contents[slot] = info.load(who);
                            }
                        }
                    }
                    Inventory inv = session.inventory;
                    for (int i = contents.length; i-- != 0;) {
                        inv.setItem(i, contents[i]);
                        contents[i] = null;
                    }
                });
            }
        } else {
            boolean[] update = new boolean[size];
            boolean b = false;
            for (int i = 0; i < size; i++)
                b |= update[i] = items[i].tick();
            final boolean changed = b;
            sessions.forEach((player, session) -> {
                if (session.opening != null) {
                    if (session.opening.tick()) session.opening = null;
                    else return;
                }
                if (changed) {
                    MenuItem item;
                    MenuItemInfo info;
                    for (int i = 0; i < size; i++)
                        if (update[i]) {
                            info = (item = items[i]).getInfo();
                            Inventory inv = session.inventory;
                            MenuItem[] visible = session.items;
                            int slot = session.slots[i];
                            boolean hidden = info.isHidden(player);
                            ItemStack is = inv.getItem(slot);
                            if (hidden) {
                                if (is != null && visible[slot] == item) {
                                    visible[slot] = null;
                                    inv.setItem(slot, null);
                                }
                            } else {
                                if (is != null) info.apply(player, is);
                                else inv.setItem(slot, info.load(player));
                                visible[slot] = item;
                            }
                        }
                }
            });
        }
    }

    private static class Session {

        private final Inventory inventory;
        private final MenuItem[] items;
        private final int[] slots;

        private OpenAnimation.Animation opening;

        private Session(Inventory inventory, MenuItem[] items,
                        int[] slots, OpenAnimation.Animation opening) {
            this.inventory = inventory;
            this.items = items;
            this.slots = slots;
            this.opening = opening;
        }
    }
}
