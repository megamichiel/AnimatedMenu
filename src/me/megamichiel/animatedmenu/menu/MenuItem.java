package me.megamichiel.animatedmenu.menu;

public final class MenuItem {

    private final ItemInfo info;
    private final int id, frameDelay, refreshDelay;
    private int frameTick, refreshTick;

    MenuItem next, previous;
    boolean removed;

    MenuItem(ItemInfo info, int id) {
        this.info = info;
        this.id = id;
        frameDelay = frameTick = Math.abs(info.getDelay(false));
        refreshDelay = refreshTick = Math.abs(info.getDelay(true));
    }

    boolean tick() {
        if (frameTick-- == 0) {
            frameTick = frameDelay;
            info.nextFrame();
        }
        return refreshTick-- == 0;
    }

    void postTick() {
        if (refreshTick == -1)
            refreshTick = refreshDelay;
    }

    boolean canRefresh() {
        return refreshTick == -1;
    }

    public void setFrameTick(int i) {
        frameTick = Math.max(i, 0);
    }

    public void setRefreshTick(int i) {
        refreshTick = Math.max(i, 0);
    }

    public void requestRefresh() {
        refreshTick = 0;
    }

    public ItemInfo getInfo() {
        return info;
    }

    public boolean isRemoved() {
        return removed;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof MenuItem && ((MenuItem) obj).info == info);
    }
}
