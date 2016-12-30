package me.megamichiel.animatedmenu.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class Delay<E> implements Predicate<E> {

    private final Map<E, AtomicLong> delays = new HashMap<>();
    private final long delay;

    public Delay(long delay) {
        this.delay = delay;
    }

    @Override
    public boolean test(E e) {
        return timeLeft(e) == 0;
    }

    public long timeLeft(E e) {
        AtomicLong l = delays.get(e);
        long time = System.currentTimeMillis();
        if (l == null) {
            delays.put(e, new AtomicLong(time + delay));
            return 0;
        }
        if (time < l.get()) return l.get() - time;
        l.set(time + delay);
        return 0;
    }

    public void remove(E e) {
        delays.remove(e);
    }
}
