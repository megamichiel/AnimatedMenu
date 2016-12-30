package me.megamichiel.animatedmenu.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryListener implements Runnable {

    private final Logger log;
    private final WatchService service;
    private final Path dir;

    private final Map<WatchEvent.Kind<?>, FileAction> actions = new HashMap<>();
    private final FileListener listener;

    private final Thread thread;
    private boolean running = true;

    public DirectoryListener(Logger log, File directory, FileListener listener) throws IOException {
        this.log = log;
        service = FileSystems.getDefault().newWatchService();
        dir = directory.toPath();
        dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        actions.put(ENTRY_CREATE, FileAction.CREATE);
        actions.put(ENTRY_MODIFY, FileAction.MODIFY);
        actions.put(ENTRY_DELETE, FileAction.DELETE);
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
        while (running) {
            try {
                WatchKey key = service.take();

                List<WatchEvent<?>> watchEvents = key.pollEvents();

                for (WatchEvent<?> event : watchEvents) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == OVERFLOW) continue;

                    File file = dir.resolve((Path) event.context()).toFile();

                    listener.fileChanged(file, actions.get(kind));
                }

                boolean valid = key.reset();
                if (!valid) break;
            } catch (InterruptedException ex) {
                // Interrupted by the plugin disabling
                if (running) {
                    log.severe("Directory change listener was interrupted by an external source!");
                    ex.printStackTrace();
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
