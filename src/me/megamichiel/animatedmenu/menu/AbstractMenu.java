package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedOpenAnimation;
import me.megamichiel.animatedmenu.animation.OpenAnimation;
import me.megamichiel.animationlib.config.AbstractConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
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
        setSlotUpdateDelay(Math.max(config.getInt("slot-update-delay", 20), 0));

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
        menuGrid.sortSlots(); // Put items with non-dynamic slot before those with it
        dynamicSlots = false;
        for (int i = 0, size = menuGrid.getSize(); i < size; i++)
            if (menuGrid.getItems()[i].getInfo().hasDynamicSlot()) {
                dynamicSlots = true;
                break;
            }
    }

    public void setSlotUpdateDelay(int slotUpdateDelay) {
        this.slotUpdateDelay = slotUpdateDelay;
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
        int[] slotToPos = new int[contents.length];
        int[] slots = new int[menuGrid.getSize()];
        Arrays.fill(slotToPos, -1);
        Arrays.fill(slots, -1);

        MenuItemInfo.SlotContext ctx = new MenuItemInfo.SlotContext(contents);
        ctx.setSlots(slots, slotToPos);
        for (int pos = 0, slot; pos < menuGrid.getSize(); pos++) {
            MenuItem item = menuGrid.getItems()[pos];
            MenuItemInfo info = item.getInfo();
            if (!info.isHidden(who)) {
                ItemStack stack = info.load(who);
                ctx.setItem(item, stack);
                slotToPos[slot = slots[pos] = info.getSlot(who, ctx)] = pos;
                contents[slot] = stack;
            }
        }
        if (emptyItem != null) {
            MenuItemInfo info = emptyItem.getInfo();
            for (int slot = 0; slot < slotToPos.length; slot++) {
                if (slotToPos[slot] == -1)
                    contents[slot] = info.load(who);
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

        sessions.put(who, new Session(inv, slots, slotToPos, opening));
    }

    void click(Player who, int slot, ClickType type) {
        Session s = sessions.get(who);
        if (s != null && s.opening == null) {
            int pos = s.slotToPos[slot];
            if (pos != -1) menuGrid.getItems()[pos].getInfo().click(who, type);
            else if (emptyItem != null) emptyItem.getInfo().click(who, type);
        }
    }

    public Set<Player> getViewers() {
        return sessions.keySet();
    }

    private static final Player[] EMPTY_PLAYER_ARRAY = new Player[0];

    public void forEachViewer(Consumer<? super Player> action) {
        for (Player player : getViewers().toArray(EMPTY_PLAYER_ARRAY))
            action.accept(player);
    }

    public void closeAll() {
        for (Player player : getViewers().toArray(EMPTY_PLAYER_ARRAY))
            player.closeInventory();
    }

    public void requestSlotUpdate() {
        slotUpdateTimer = 0;
    }

    public void tick() {
        MenuItem[] items = menuGrid.getItems();
        int size = menuGrid.getSize();
        if (dynamicSlots) {
            boolean updateSlots = slotUpdateTimer-- == 0;
            if (updateSlots) slotUpdateTimer = slotUpdateDelay;
            boolean changed = updateSlots;
            for (int i = 0; i < size; i++) changed |= items[i].tick();
            if (changed) {
                int invSize = menuType.getSize();
                ItemStack[] contents = new ItemStack[invSize];
                MenuItemInfo.SlotContext ctx = new MenuItemInfo.SlotContext(contents);
                int[] weights = ctx.getWeights();
                sessions.forEach((who, session) -> {
                    if (session.opening != null) {
                        if (session.opening.tick()) session.opening = null;
                        else return;
                    }
                    int[] slotToPos = session.slotToPos,
                              slots = session.slots;
                    ctx.setSlots(slots, slotToPos);
                    MenuItem item;
                    for (int i = 0; i < size; i++) {
                        if (!(item = items[i]).getInfo().isHidden(who)) {
                            ItemStack stack = item.getInfo().load(who);
                            int slot = slots[i];
                            if (updateSlots) {
                                slotToPos[slot] = -1;
                                ctx.setItem(item, stack);
                                slot = slots[i] = item.getInfo().getSlot(who, ctx);
                            }
                            slotToPos[slot] = i;
                            contents[slot] = stack;
                        } else slotToPos[i] = -1;
                    }
                    if (emptyItem != null) {
                        MenuItemInfo info = emptyItem.getInfo();
                        for (int slot = 0; slot < items.length; slot++)
                            if (slotToPos[slot] == -1)
                                contents[slot] = info.load(who);
                    }
                    Inventory inv = session.inventory;
                    for (int i = contents.length - 1; i >= 0; --i) {
                        inv.setItem(i, contents[i]);
                        contents[i] = null;
                        weights[i] = 0;
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
                    MenuItemInfo info;
                    for (int i = 0; i < size; i++) {
                        if (update[i]) {
                            info = items[i].getInfo();
                            Inventory inv = session.inventory;
                            int[] visible = session.slotToPos;
                            int slot = session.slots[i];
                            boolean hidden = info.isHidden(player);
                            ItemStack is = inv.getItem(slot);
                            if (hidden) {
                                if (is != null && visible[slot] == i) {
                                    visible[slot] = -1;
                                    inv.setItem(slot, null);
                                }
                            } else {
                                if (is != null) info.apply(player, is);
                                else inv.setItem(slot, info.load(player));
                                visible[slot] = i;
                            }
                        }
                    }
                }
            });
        }
    }

    private static class Session {

        private final Inventory inventory;
        private final int[] slots, slotToPos;

        private OpenAnimation.Animation opening;

        private Session(Inventory inventory, int[] slots,
                        int[] slotToPos, OpenAnimation.Animation opening) {
            this.inventory = inventory;
            this.slots = slots;
            this.slotToPos = slotToPos;
            this.opening = opening;
        }
    }
}
