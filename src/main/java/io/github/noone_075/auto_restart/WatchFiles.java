package io.github.noone_075.auto_restart;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public class WatchFiles {
    private final Plugin plugin;
    private final Consumer<Path> callback;
    private final BooleanSupplier enabledSupplier;
    private final LongSupplier pausedUntilSupplier;

    private final Map<Path, WatchType> watchConfig = new ConcurrentHashMap<>();
    private final Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>();
    private WatchService watchService;
    private Thread watchThread;

    public WatchFiles(Plugin plugin,
                      Consumer<Path> callback,
                      BooleanSupplier enabledSupplier,
                      LongSupplier pausedUntilSupplier) {
        this.plugin = plugin;
        this.callback = callback;
        this.enabledSupplier = enabledSupplier;
        this.pausedUntilSupplier = pausedUntilSupplier;
    }

    public synchronized void setWatchConfig(Map<Path, WatchType> config) {
        watchConfig.clear();
        watchConfig.putAll(config);
    }

    public synchronized void startWatching() {
        stopWatching();

        if (watchConfig.isEmpty()) {
            plugin.getLogger().warning("No watch rules loaded. Watching is disabled until configuration is added.");
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start watch service: " + e.getMessage());
            return;
        }

        for (Map.Entry<Path, WatchType> entry : watchConfig.entrySet()) {
            Path path = entry.getKey();
            WatchType type = entry.getValue();

            try {
                if (type == WatchType.FILE) {
                    Path parent = path.getParent();
                    if (parent != null && Files.exists(parent)) {
                        registerWatch(parent);
                    } else {
                        plugin.getLogger().warning("Unable to watch parent directory for file: " + path);
                    }
                } else if (type == WatchType.DIR) {
                    if (Files.isDirectory(path)) {
                        registerWatch(path);
                    } else {
                        plugin.getLogger().warning("Directory watch path is not a directory: " + path);
                    }
                } else if (type == WatchType.RDIR) {
                    if (Files.exists(path)) {
                        registerRecursive(path);
                    } else {
                        plugin.getLogger().warning("Recursive directory path does not exist: " + path);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to register watch for " + path + ": " + e.getMessage());
            }
        }

        watchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                processWatchEvents();
            }
        }, "AutoRestart-Watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    public synchronized void stopWatching() {
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
            watchThread = null;
        }
        watchKeys.clear();
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
            watchService = null;
        }
    }

    private void registerWatch(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Not a directory: " + directory);
        }

        WatchKey key = directory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        watchKeys.put(key, directory);
        plugin.getLogger().info("Watching directory: " + directory);
    }

    private void registerRecursive(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerWatch(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void processWatchEvents() {
        while (!Thread.currentThread().isInterrupted() && watchService != null) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }

            Path watchedDir = watchKeys.get(key);
            if (watchedDir == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> rawEvent : key.pollEvents()) {
                WatchEvent.Kind<?> kind = rawEvent.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                Path relativePath = (Path) rawEvent.context();
                Path absolutePath = watchedDir.resolve(relativePath).toAbsolutePath().normalize();

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(absolutePath) && isRecursiveWatched(absolutePath)) {
                        try {
                            registerRecursive(absolutePath);
                        } catch (IOException e) {
                            plugin.getLogger().warning("Failed to register new recursive directory: " + absolutePath + " -> " + e.getMessage());
                        }
                    }
                }

                if (shouldTriggerRestart(absolutePath)) {
                    plugin.getLogger().info("Detected change on watched path: " + absolutePath);
                    callback.accept(absolutePath);
                }
            }

            key.reset();
        }
    }

    private boolean isRecursiveWatched(Path path) {
        for (Map.Entry<Path, WatchType> entry : watchConfig.entrySet()) {
            if (entry.getValue() == WatchType.RDIR && path.startsWith(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldTriggerRestart(Path changedPath) {
        if (!enabledSupplier.getAsBoolean()) {
            return false;
        }
        if (pausedUntilSupplier.getAsLong() > System.currentTimeMillis()) {
            return false;
        }

        for (Map.Entry<Path, WatchType> entry : watchConfig.entrySet()) {
            Path configuredPath = entry.getKey();
            WatchType type = entry.getValue();
            if (type == WatchType.FILE) {
                if (changedPath.equals(configuredPath)) {
                    return true;
                }
            } else if (type == WatchType.DIR) {
                Path parent = changedPath.getParent();
                if (parent != null && parent.equals(configuredPath)) {
                    return true;
                }
            } else if (type == WatchType.RDIR) {
                if (changedPath.startsWith(configuredPath)) {
                    return true;
                }
            }
        }
        return false;
    }
}
