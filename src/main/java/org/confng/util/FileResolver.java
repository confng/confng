package org.confng.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for resolving and reading configuration files from both
 * filesystem and classpath (including JAR resources).
 * 
 * <p>This class provides a centralized way to handle file loading that works
 * consistently across different deployment scenarios:</p>
 * <ul>
 *   <li>Files on the filesystem</li>
 *   <li>Files in the classpath (e.g., src/main/resources)</li>
 *   <li>Files inside JAR files</li>
 * </ul>
 * 
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.0.3
 */
public class FileResolver {
    
    /**
     * Result of file resolution containing the resolved path and metadata.
     */
    public static class ResolvedFile {
        private final Path path;
        private final String fileName;
        private final boolean existsInFilesystem;
        private final boolean existsInClasspath;
        
        private ResolvedFile(Path path, String fileName, boolean existsInFilesystem, boolean existsInClasspath) {
            this.path = path;
            this.fileName = fileName;
            this.existsInFilesystem = existsInFilesystem;
            this.existsInClasspath = existsInClasspath;
        }
        
        /**
         * Gets the resolved path that can be used for reading the file.
         * For JAR resources, this will be a temporary file.
         * 
         * @return the resolved path
         */
        public Path getPath() {
            return path;
        }
        
        /**
         * Gets the original filename (without path).
         * 
         * @return the filename
         */
        public String getFileName() {
            return fileName;
        }
        
        /**
         * Checks if the file exists (either in filesystem or classpath).
         * 
         * @return true if the file exists
         */
        public boolean exists() {
            return path != null && Files.exists(path);
        }
        
        /**
         * Checks if the file was found in the filesystem.
         * 
         * @return true if found in filesystem
         */
        public boolean isFromFilesystem() {
            return existsInFilesystem;
        }
        
        /**
         * Checks if the file was found in the classpath.
         * 
         * @return true if found in classpath
         */
        public boolean isFromClasspath() {
            return existsInClasspath;
        }
    }
    
    /**
     * Resolves a file path by checking filesystem first, then classpath.
     * For JAR resources, creates a temporary file that will be deleted on JVM exit.
     * 
     * @param filePath the file path to resolve
     * @return a ResolvedFile containing the resolved path and metadata, or null if not found
     */
    public static ResolvedFile resolve(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        
        // Try filesystem first
        Path fsPath = Paths.get(filePath);
        if (Files.exists(fsPath)) {
            String fileName = fsPath.getFileName().toString();
            return new ResolvedFile(fsPath, fileName, true, false);
        }
        
        // Try classpath
        try {
            URL resource = FileResolver.class.getClassLoader().getResource(filePath);
            if (resource != null) {
                // Extract filename from the resource path
                String resourcePath = resource.getPath();
                String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                
                // For JAR resources, copy to a temp file
                if (resource.getProtocol().equals("jar")) {
                    Path tempFile = Files.createTempFile("confng-", "-" + fileName);
                    tempFile.toFile().deleteOnExit();
                    try (InputStream in = resource.openStream()) {
                        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return new ResolvedFile(tempFile, fileName, false, true);
                } else {
                    // For file resources, use the URI directly
                    Path path = Paths.get(resource.toURI());
                    return new ResolvedFile(path, fileName, false, true);
                }
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        
        return null;
    }
    
    /**
     * Opens an InputStream for reading a file from either filesystem or classpath.
     * 
     * @param filePath the file path to read
     * @return an InputStream for reading the file
     * @throws IOException if the file cannot be opened or doesn't exist
     */
    public static InputStream openInputStream(String filePath) throws IOException {
        ResolvedFile resolved = resolve(filePath);
        if (resolved == null || !resolved.exists()) {
            throw new IOException("File not found: " + filePath);
        }
        return Files.newInputStream(resolved.getPath());
    }
    
    /**
     * Opens a Reader for reading a file from either filesystem or classpath.
     * 
     * @param filePath the file path to read
     * @return a Reader for reading the file
     * @throws IOException if the file cannot be opened or doesn't exist
     */
    public static Reader openReader(String filePath) throws IOException {
        ResolvedFile resolved = resolve(filePath);
        if (resolved == null || !resolved.exists()) {
            throw new IOException("File not found: " + filePath);
        }
        return Files.newBufferedReader(resolved.getPath());
    }
    
    /**
     * Checks if a file exists in either filesystem or classpath.
     * 
     * @param filePath the file path to check
     * @return true if the file exists
     */
    public static boolean exists(String filePath) {
        ResolvedFile resolved = resolve(filePath);
        return resolved != null && resolved.exists();
    }
    
    /**
     * Gets the filename (without path) from a file path.
     * 
     * @param filePath the file path
     * @return the filename, or null if the path is invalid
     */
    public static String getFileName(String filePath) {
        ResolvedFile resolved = resolve(filePath);
        return resolved != null ? resolved.getFileName() : null;
    }
}

