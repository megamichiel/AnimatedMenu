package me.megamichiel.animatedmenu.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryListener implements Runnable {

    private static final Map<WatchEvent.Kind<?>, FileAction> ACTIONS = new HashMap<>();

    static {
        ACTIONS.put(ENTRY_CREATE, FileAction.CREATE);
        ACTIONS.put(ENTRY_MODIFY, FileAction.MODIFY);
        ACTIONS.put(ENTRY_DELETE, FileAction.DELETE);
    }

    private final Logger log;
    private final WatchService service;
    private final Path dir;

    private final FileListener listener;

    private final Thread thread;
    private boolean running = true;

    public DirectoryListener(Logger log, File directory, FileListener listener) throws IOException {
        this.log = log;
        service = FileSystems.getDefault().newWatchService();
        dir = directory.toPath();
        dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        this.listener = listener;
        (thread = new Thread(this)).start();
    }

    public void stop() {
        running = false;
        thread.interrupt();
    }

    private final Cache<String, Long> lastModified = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        while (running) {
            try {
                WatchKey key = service.take();

                List<WatchEvent<?>> watchEvents = key.pollEvents();

                for (WatchEvent<?> event : watchEvents) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == OVERFLOW) continue;

                    try {
                        File file = dir.resolve((Path) event.context()).toFile();

                        FileAction action = ACTIONS.get(kind);
                        if (action == FileAction.MODIFY) {
                            long lastMod = file.lastModified();
                            Long mod = lastModified.getIfPresent(file.getName());
                            if (mod != null && mod == lastMod) continue;
                            lastModified.put(file.getName(), lastMod);
                        }
                        listener.fileChanged(file, action);
                    } catch (RuntimeException ex) {
                        ex.printStackTrace();
                    }
                }

                if (!key.reset()) break;
            } catch (InterruptedException ex) {
                // Interrupted by the plugin disabling
                if (running) {
                    log.log(Level.SEVERE, "Directory change listener was interrupted by an external source!", ex);
                }
            }
        }
    }

    public enum FileAction {
        CREATE, MODIFY, DELETE
    }

    public interface FileListener {
        void fileChanged(File file, FileAction action);
    }
}
