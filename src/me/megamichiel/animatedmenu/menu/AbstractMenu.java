package me.megamichiel.animatedmenu.menu;

import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animatedmenu.animation.AnimatedOpenAnimation;
import me.megamichiel.animatedmenu.animation.OpenAnimation;
import me.megamichiel.animatedmenu.util.PlayerMap;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.config.AbstractConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMenu {

    private static final ItemStack[] EMPTY_ITEM_ARRAY = new ItemStack[0];

    protected final Nagger nagger;
    protected final String name;
    protected final MenuType menuType;
    protected final MenuLoader loader;
    protected final MenuGrid menuGrid;

    private final AnimatedOpenAnimation openAnimation;

    protected AnimatedMenuPlugin plugin;
    private boolean dynamicSlots;

    private int slotUpdateDelay, slotUpdateTimer;

    protected final PlayerMap<Session> items = new PlayerMap<>();
    protected final PlayerMap<OpenAnimation.Animation> opening = new PlayerMap<>();

    protected AbstractMenu(Nagger nagger, String name,
                           MenuType menuType, MenuLoader loader) {
        this.nagger = nagger;
        this.name = name;
        this.menuType = menuType;
        this.loader = loader;
        menuGrid = new MenuGrid(this, menuType.getSize());
        openAnimation = new AnimatedOpenAnimation(menuType);
    }

    private AbstractConfig config;

    public void dankLoad(AbstractConfig config) {
        int delay = config.getInt("slot-update-delay", 20);
        if (delay < 0) delay = 0;
        slotUpdateDelay = delay;

        if (plugin != null) {
            double speed = config.getDouble("animation-speed", 1);
            openAnimation.init(plugin, speed <= 0 ? 1 : speed);
            openAnimation.load(plugin, config, "open-animation");
        } else this.config = config;
    }

    public void init(AnimatedMenuPlugin plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            items.init(plugin);

            if (config != null) {
                double speed = config.getDouble("animation-speed", 1);
                openAnimation.init(plugin, speed <= 0 ? 1 : speed);
                openAnimation.load(plugin, config, "open-animation");
            }
        }
        dynamicSlots = false;
        for (int i = 0; i < menuGrid.getSize(); i++)
            if (menuGrid.getItems()[i].hasDynamicSlot()) {
                dynamicSlots = true;
                break;
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

    protected void removeViewer(Player player) {
        items.remove(player);
        opening.remove(player);
    }

    protected void setup(Player who, Inventory inv) {
        ItemStack[] contents = new ItemStack[inv.getSize()];
        IMenuItem[] items = new IMenuItem[contents.length];
        for (int slot = 0, result; slot < menuGrid.getSize(); slot++) {
            IMenuItem item = menuGrid.getItems()[slot];
            if (!item.isHidden(plugin, who)) {
                ItemStack stack = item.load(nagger, who);
                items[result = item.getSlot(who, contents, stack, true)] = item;
                contents[result] = stack;
            }
        }
        OpenAnimation anim = openAnimation.next();
        if (anim != null) opening.put(who, anim.newAnimation(slots -> {
            for (int slot : slots) if (slot != -1 && contents[slot] != null)
                inv.setItem(slot, contents[slot]);
        }));
        else {
            inv.setContents(contents);
            this.items.put(who, new Session(inv, items));
        }
    }

    public IMenuItem getItem(Player who, int slot) {
        Session s = items.get(who);
        return s == null ? null : s.items[slot];
    }

    public Set<Player> getViewers() {
        return items.keySet();
    }

    public void tick() {
        if (plugin == null) return;
        for (Iterator<OpenAnimation.Animation> it = opening.values().iterator(); it.hasNext(); )
            if (it.next().tick()) it.remove();
        IMenuItem[] items = menuGrid.getItems();
        int size = menuGrid.getSize();
        if (dynamicSlots) {
            boolean updateSlots = slotUpdateTimer-- == 0;
            if (updateSlots) slotUpdateTimer = slotUpdateDelay;
            boolean changed = updateSlots;
            for (int i = 0; i < size; i++) changed |= items[i].tick();
            if (changed) {
                for (Session session : this.items.values())
                    Arrays.fill(session.items, null);
                ItemStack[] contents = new ItemStack[items.length];
                for (Map.Entry<Player, Session> entry : this.items.entrySet()) {
                    Player player = entry.getKey();
                    IMenuItem[] visible = entry.getValue().items;
                    IMenuItem item;
                    for (int i = 0; i < size; i++)
                        if (!(item = items[i]).isHidden(plugin, player)) {
                            ItemStack stack = item.getItem(nagger, player);
                            int slot = item.getSlot(player, contents, stack, updateSlots);
                            visible[slot] = item;
                            contents[slot] = stack;
                        }
                    Inventory inv = entry.getValue().inventory;
                    for (int i = contents.length; i-- != 0;) {
                        inv.setItem(i, contents[i]);
                        contents[i] = null;
                    }
                }
            }
        } else for (int index = 0; index < size; index++) {
            IMenuItem item = items[index];
            if (item.tick()) {
                for (Map.Entry<Player, Session> entry : this.items.entrySet()) {
                    Player player = entry.getKey();
                    Inventory inv = entry.getValue().inventory;
                    IMenuItem[] visible = entry.getValue().items;
                    int slot = item.getSlot(player, EMPTY_ITEM_ARRAY, null, false);
                    boolean hidden = item.isHidden(plugin, player);
                    ItemStack is = inv.getItem(slot);
                    if (hidden) {
                        if (is != null && visible[slot] == item) {
                            visible[slot] = null;
                            inv.setItem(slot, null);
                        }
                    } else {
                        if (is != null) item.apply(nagger, player, is);
                        else inv.setItem(slot, item.load(nagger, player));
                        visible[slot] = item;
                    }
                }
            }
        }
    }

    private static class Session {

        private final Inventory inventory;
        private final IMenuItem[] items;

        private Session(Inventory inventory, IMenuItem[] items) {
            this.inventory = inventory;
            this.items = items;
        }
    }
}
