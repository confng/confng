package org.confng.reload;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Background service that watches configuration files for changes.
 *
 * <p>This service uses Java's WatchService API to monitor directories containing
 * configuration files. When a file changes, it triggers a reload of the configuration.</p>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Slf4j
public class FileWatchService implements AutoCloseable {
    
    private final WatchService watchService;
    private final Map<WatchKey, Path> watchKeyToDir = new ConcurrentHashMap<>();
    private final Set<Path> watchedFiles = ConcurrentHashMap.newKeySet();
    private final Consumer<Path> onFileChange;
    private final ExecutorService executor;
    private volatile boolean running = false;
    private final long debounceMs;
    private final Map<Path, Long> lastModified = new ConcurrentHashMap<>();
    
    /**
     * Creates a new file watch service.
     *
     * @param onFileChange callback invoked when a watched file changes
     * @param debounceMs minimum time between reload triggers for the same file
     * @throws IOException if the watch service cannot be created
     */
    public FileWatchService(Consumer<Path> onFileChange, long debounceMs) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.onFileChange = onFileChange;
        this.debounceMs = debounceMs;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ConfNG-FileWatcher");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Creates a new file watch service with default 1 second debounce.
     */
    public FileWatchService(Consumer<Path> onFileChange) throws IOException {
        this(onFileChange, 1000);
    }
    
    /**
     * Registers a file to be watched for changes.
     *
     * @param filePath the file to watch
     * @throws IOException if the file's directory cannot be watched
     */
    public void watchFile(Path filePath) throws IOException {
        Path absolutePath = filePath.toAbsolutePath();
        Path dir = absolutePath.getParent();
        
        if (dir == null) {
            log.warn("Cannot watch file without parent directory: {}", filePath);
            return;
        }
        
        if (!Files.exists(dir)) {
            log.warn("Directory does not exist, cannot watch: {}", dir);
            return;
        }
        
        watchedFiles.add(absolutePath);
        
        // Check if we're already watching this directory
        boolean alreadyWatching = watchKeyToDir.values().stream()
                .anyMatch(p -> p.equals(dir));
        
        if (!alreadyWatching) {
            WatchKey key = dir.register(watchService, 
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);
            watchKeyToDir.put(key, dir);
            log.debug("Watching directory: {}", dir);
        }
        
        log.info("Watching file for changes: {}", absolutePath);
    }
    
    /**
     * Starts the file watching service.
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::watchLoop);
        log.info("File watch service started");
    }
    
    /**
     * Stops the file watching service.
     */
    public void stop() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("File watch service stopped");
    }
    
    private void watchLoop() {
        while (running) {
            try {
                WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);
                if (key == null) {
                    continue;
                }
                
                Path dir = watchKeyToDir.get(key);
                if (dir == null) {
                    key.reset();
                    continue;
                }
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path changedFile = dir.resolve(pathEvent.context());
                    
                    if (watchedFiles.contains(changedFile)) {
                        // Debounce check
                        long now = System.currentTimeMillis();
                        Long lastMod = lastModified.get(changedFile);
                        if (lastMod == null || (now - lastMod) > debounceMs) {
                            lastModified.put(changedFile, now);
                            log.info("Detected change in: {}", changedFile);
                            onFileChange.accept(changedFile);
                        }
                    }
                }
                
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in file watch loop", e);
            }
        }
    }

    @Override
    public void close() {
        stop();
        try {
            watchService.close();
        } catch (IOException e) {
            log.warn("Error closing watch service", e);
        }
    }

    /**
     * Checks if the service is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Gets the set of files being watched.
     */
    public Set<Path> getWatchedFiles() {
        return Collections.unmodifiableSet(watchedFiles);
    }
}

