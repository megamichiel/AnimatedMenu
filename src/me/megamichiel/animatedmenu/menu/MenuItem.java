package me.megamichiel.animatedmenu.menu;

public class MenuItem {

    private final MenuItemInfo info;
    private final int frameDelay, refreshDelay;
    private int frameTick, refreshTick;

    MenuItem(MenuItemInfo info) {
        this.info = info;
        frameDelay = frameTick = info.getDelay(false);
        refreshDelay = refreshTick = info.getDelay(true);
    }

    boolean tick() {
        if (frameTick-- == 0) {
            frameTick = frameDelay;
            info.nextFrame();
        }
        if (refreshTick-- == 0) {
            refreshTick = refreshDelay;
            return true;
        }
        return false;
    }

    public void requestRefresh() {
        refreshTick = 0;
    }

    public MenuItemInfo getInfo() {
        return info;
    }
}
