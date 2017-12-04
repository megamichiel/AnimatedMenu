package me.megamichiel.animatedmenu.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
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
        (dir = directory.toPath()).register(service = FileSystems.getDefault().newWatchService(), ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        this.listener = listener;
        (thread = new Thread(this)).start();
    }

    public void stop() {
        running = false;
        thread.interrupt();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        Map<String, Long> lastModified = new HashMap<>();
        WatchKey key = null;
        do {
            long time = System.currentTimeMillis();
            lastModified.entrySet().removeIf($entry -> time - $entry.getValue() >= 60_000);
            try {
                for (WatchEvent<?> event : (key = service.take()).pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == OVERFLOW) {
                        continue;
                    }

                    try {
                        File file = dir.resolve((Path) event.context()).toFile();

                        FileAction action = ACTIONS.get(kind);
                        if (action == FileAction.MODIFY) {
                            Long lastMod = file.lastModified();
                            if (lastMod.equals(lastModified.put(file.getName(), lastMod))) {
                                continue;
                            }
                        }
                        listener.fileChanged(file, action);
                    } catch (RuntimeException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (InterruptedException ex) {
                // Interrupted by the plugin disabling
                if (running) {
                    log.log(Level.SEVERE, "File change listener was interrupted by an external source!", ex);
                    running = false;
                }
            }
        } while (running && key.reset());
    }

    public enum FileAction {
        CREATE, MODIFY, DELETE
    }

    public interface FileListener {
        void fileChanged(File file, FileAction action);
    }
}
