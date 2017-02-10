package me.megamichiel.animatedmenu.util;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.placeholder.ctx.ParsingContext;
import me.megamichiel.animationlib.util.ParsingNagger;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class Delay {

    private final Map<Player, AtomicLong> delays = new HashMap<>();
    private final StringBundle delayMessage;
    private final long delay;
    private long result;

    public Delay(Nagger nagger, String delayMessage, long delay) {
        if (delayMessage != null) {
            this.delayMessage = StringBundle.parse(new MessageNagger(nagger), delayMessage).colorAmpersands();
        } else this.delayMessage = null;
        this.delay = delay;
    }

    public boolean test(Player player) {
        AtomicLong l = delays.get(player);
        long time = System.currentTimeMillis();
        if (l == null) {
            delays.put(player, new AtomicLong(time + delay));
            return true;
        }
        if (time < l.get()) {
            if (delayMessage != null) {
                result = l.get() - time;
                player.sendMessage(delayMessage.toString(player));
            }
            return false;
        }
        l.set(time + delay);
        return true;
    }

    public void remove(Player player) {
        delays.remove(player);
    }

    private class MessageNagger implements ParsingNagger, ParsingContext {

        private final Nagger nagger;

        private MessageNagger(Nagger nagger) {
            this.nagger = nagger;
        }

        @Override
        public ParsingContext context() {
            return this;
        }

        @Override
        public void nag(String s) {
            nagger.nag(s);
        }

        @Override
        public void nag(Throwable throwable) {
            nagger.nag(throwable);
        }

        @Override
        public IPlaceholder<?> parse(String s) {
            switch (s) {
                case "hoursleft": return (n, p) -> result / 3600_000;
                case "minutesleft": return (n, p) -> (result / 60_000) % 60;
                case "secondsleft":  return (n, p) -> (result / 1_000) % 60;
                case "ticksleft": return (n, p) -> (result / 50) % 20;
                default:
                    return nagger instanceof ParsingNagger ? ((ParsingNagger) nagger).context().parse(s) : null;
            }
        }
    }
}
