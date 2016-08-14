package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.util.PlayerMap;
import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractMenu {

    private static final ItemStack[] EMPTY_ITEM_ARRAY = new ItemStack[0];

    protected final Nagger nagger;
    protected final String name;
    protected final MenuType menuType;
    protected final MenuLoader loader;
    protected final MenuGrid menuGrid;

    protected AnimatedMenuPlugin plugin;
    private boolean dynamicSlots;

    private int slotUpdateDelay, slotUpdateTimer;

    protected final PlayerMap<IMenuItem[]> items = new PlayerMap<>();

    protected AbstractMenu(Nagger nagger, String name,
                           MenuType menuType, MenuLoader loader) {
        this.nagger = nagger;
        this.name = name;
        this.menuType = menuType;
        this.loader = loader;
        menuGrid = new MenuGrid(this, menuType.getSize());
    }

    public void setSlotUpdateDelay(int slotUpdateDelay) {
        this.slotUpdateDelay = slotUpdateDelay;
    }

    public void init(AnimatedMenuPlugin plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            items.init(plugin);
        }
        for (int i = 0; i < menuGrid.getSize(); i++) {
            if (menuGrid.getItems()[i].hasDynamicSlot()) {
                dynamicSlots = true;
                break;
            }
        }
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

    protected abstract Iterator<Map.Entry<Player, Inventory>> getViewers();

    public IMenuItem getItem(Player who, int slot) {
        IMenuItem[] i = items.get(who);
        return i == null ? null : i[slot];
    }

    public void remove(Player player) {
        items.remove(player);
    }

    public void tick() {
        if (plugin == null) return;
        IMenuItem[] items = menuGrid.getItems();
        int size = menuGrid.getSize();
        if (dynamicSlots) {
            boolean changed = false;
            for (int i = 0; i < size; i++) changed |= items[i].tick();
            if (changed) {
                for (IMenuItem[] menuItems : this.items.values())
                    Arrays.fill(menuItems, null);
                ItemStack[] contents = new ItemStack[items.length];
                Iterator<Map.Entry<Player, Inventory>> viewers = getViewers();
                boolean updateSlots = slotUpdateTimer-- == 0;
                if (updateSlots) slotUpdateTimer = slotUpdateDelay;
                while (viewers.hasNext()) {
                    Map.Entry<Player, Inventory> entry = viewers.next();
                    Player player = entry.getKey();
                    Inventory inv = entry.getValue();
                    IMenuItem[] visible = this.items.get(player);
                    if (visible == null) this.items.put(player,
                            visible = new IMenuItem[inv.getSize()]);
                    IMenuItem item;
                    for (int i = 0; i < size; i++)
                        if (!(item = items[i]).isHidden(plugin, player)) {
                            ItemStack stack = item.getItem(nagger, player);
                            int slot = item.getSlot(player, contents, stack, updateSlots);
                            visible[slot] = item;
                            contents[slot] = stack;
                        }
                    for (int i = 0; i < contents.length; i++) {
                        inv.setItem(i, contents[i]);
                        contents[i] = null;
                    }
                }
            }
        } else for (int index = 0; index < size; index++) {
            IMenuItem item = items[index];
            if (item.tick()) {
                Iterator<Map.Entry<Player, Inventory>> viewers = getViewers();
                while (viewers.hasNext()) {
                    Map.Entry<Player, Inventory> entry = viewers.next();
                    Player player = entry.getKey();
                    Inventory inv = entry.getValue();
                    int slot = item.getSlot(player, EMPTY_ITEM_ARRAY, null, false);
                    boolean hidden = item.isHidden(plugin, player);
                    ItemStack is = inv.getItem(slot);
                    if (hidden && is != null) {
                        inv.setItem(slot, null);
                        continue;
                    } else if (!hidden && is == null) {
                        inv.setItem(slot, item.load(nagger, player));
                        is = inv.getItem(slot);
                    }
                    IMenuItem[] i = this.items.get(player);
                    if (i == null) this.items.put(player, i = new IMenuItem[inv.getSize()]);
                    if (!hidden) {
                        item.apply(nagger, player, is);
                        i[slot] = item;
                    } else {
                        if (i[slot] == item) i[slot] = null; // Only replace if the same item
                    }
                }
            }
        }
    }
}
