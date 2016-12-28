package me.megamichiel.animatedmenu.menu;

class MenuItem {

    private final MenuItemInfo info;
    private final int frameDelay, refreshDelay;
    private int frameTick, refreshTick;

    MenuItem(MenuItemInfo info) {
        this.info = info;
        frameDelay = info.getDelay(false);
        refreshDelay = info.getDelay(true);
    }

    boolean tick() {
        if (frameTick++ == frameDelay) {
            frameTick = 0;
            info.nextFrame();
        }
        if (refreshTick++ == refreshDelay) {
            refreshTick = 0;
            return true;
        }
        return false;
    }

    MenuItemInfo getInfo() {
        return info;
    }
}
