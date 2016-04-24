package me.megamichiel.animatedmenu.util;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Logger;

public class DirectoryListener implements Runnable {

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

    public void stop()
    {
        running = false;
        thread.interrupt();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        while (running)
        {
            try
            {
                WatchKey key = service.take();

                for (WatchEvent<?> event : key.pollEvents())
                {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == OVERFLOW)
                    {
                        continue;
                    }
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;

                    Path path = ev.context();

                    Path child = dir.resolve(path);
                    File file = child.toFile();

                    FileAction action = kind == ENTRY_CREATE ? FileAction.CREATE : kind == ENTRY_MODIFY ? FileAction.MODIFY : FileAction.DELETE;

                    listener.fileChanged(file, action);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
            catch (InterruptedException ex) {
                // Interrupted by the plugin disabling
                if (running) {
                    log.severe("Directory change listener was interrupted by an external source!");
                    ex.printStackTrace();
                }
            }
        }
    }

    public enum FileAction
    {
        CREATE, MODIFY, DELETE
    }

    public interface FileListener {

        void fileChanged(File file, FileAction action);
    }
}
