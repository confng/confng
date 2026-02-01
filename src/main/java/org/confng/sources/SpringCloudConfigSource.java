package org.confng.sources;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration source that fetches configuration from Spring Cloud Config Server.
 *
 * <p>This source connects to a Spring Cloud Config Server and retrieves configuration
 * for a specific application, profile, and label.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Simple usage
 * SpringCloudConfigSource source = new SpringCloudConfigSource(
 *     "http://config-server:8888", "myapp", "production");
 * ConfNG.addSource(source);
 * 
 * // With authentication and custom label
 * SpringCloudConfigSource source = SpringCloudConfigSource.builder()
 *     .serverUrl("http://config-server:8888")
 *     .application("myapp")
 *     .profile("production")
 *     .label("main")
 *     .username("user")
 *     .password("secret")
 *     .build();
 * ConfNG.addSource(source);
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Slf4j
public class SpringCloudConfigSource implements ConfigSource {
    
    private final String serverUrl;
    private final String application;
    private final String profile;
    private final String label;
    private final String username;
    private final String password;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final long cacheTimeoutMs;
    private final int priority;
    private final int maxRetries;
    private final long retryDelayMs;
    private final Gson gson = new Gson();

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile long lastFetchTime = 0;
    private volatile int consecutiveFailures = 0;
    
    /**
     * Creates a new Spring Cloud Config source with default settings.
     */
    public SpringCloudConfigSource(String serverUrl, String application, String profile) {
        this(serverUrl, application, profile, "main", null, null, 5000, 10000, 30000, 35, 3, 1000);
    }

    /**
     * Creates a new Spring Cloud Config source with custom settings.
     */
    public SpringCloudConfigSource(String serverUrl, String application, String profile,
                                    String label, String username, String password,
                                    int connectTimeoutMs, int readTimeoutMs,
                                    long cacheTimeoutMs, int priority) {
        this(serverUrl, application, profile, label, username, password,
             connectTimeoutMs, readTimeoutMs, cacheTimeoutMs, priority, 3, 1000);
    }

    /**
     * Creates a new Spring Cloud Config source with custom settings including retry configuration.
     */
    public SpringCloudConfigSource(String serverUrl, String application, String profile,
                                    String label, String username, String password,
                                    int connectTimeoutMs, int readTimeoutMs,
                                    long cacheTimeoutMs, int priority,
                                    int maxRetries, long retryDelayMs) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalArgumentException("Server URL cannot be null or empty");
        }
        if (application == null || application.isEmpty()) {
            throw new IllegalArgumentException("Application cannot be null or empty");
        }
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.application = application;
        this.profile = profile;
        this.label = label;
        this.username = username;
        this.password = password;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.cacheTimeoutMs = cacheTimeoutMs;
        this.priority = priority;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }
    
    @Override
    public String getName() {
        return "SpringCloudConfig:" + serverUrl + "/" + application + "/" + profile;
    }
    
    @Override
    public Optional<String> get(String key) {
        ensureFetched();
        return Optional.ofNullable(cache.get(key));
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    private synchronized void ensureFetched() {
        long now = System.currentTimeMillis();
        if (lastFetchTime > 0 && (now - lastFetchTime) < cacheTimeoutMs) {
            return;
        }

        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("Retry attempt {} of {} for Spring Cloud Config {}/{}",
                             attempt, maxRetries, application, profile);
                    Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                }
                fetchConfig();
                consecutiveFailures = 0;
                lastFetchTime = now;
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Fetch interrupted for Spring Cloud Config {}/{}", application, profile);
                break;
            } catch (Exception e) {
                lastException = e;
                log.warn("Fetch attempt {} failed for Spring Cloud Config {}/{}: {}",
                         attempt + 1, application, profile, e.getMessage());
            }
        }

        consecutiveFailures++;
        lastFetchTime = now;
        log.error("Failed to fetch config from Spring Cloud Config {}/{} after {} attempts",
                  application, profile, maxRetries + 1, lastException);
    }
    
    private void fetchConfig() throws Exception {
        // Spring Cloud Config Server URL format: /{application}/{profile}/{label}
        String url = String.format("%s/%s/%s/%s", serverUrl, application, profile, label);
        
        log.info("Fetching configuration from Spring Cloud Config: {}", url);
        
        URL configUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) configUrl.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Accept", "application/json");
            
            if (username != null && password != null) {
                String credentials = username + ":" + password;
                String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("Spring Cloud Config request failed with code: " + responseCode);
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            parseConfigResponse(response.toString());
            log.info("Successfully fetched {} config values from Spring Cloud Config", cache.size());
        } finally {
            conn.disconnect();
        }
    }

    private void parseConfigResponse(String json) {
        cache.clear();
        JsonObject root = gson.fromJson(json, JsonObject.class);

        // Spring Cloud Config returns propertySources array
        JsonArray propertySources = root.getAsJsonArray("propertySources");
        if (propertySources == null) {
            return;
        }

        // Process in reverse order so higher priority sources override lower ones
        for (int i = propertySources.size() - 1; i >= 0; i--) {
            JsonObject propertySource = propertySources.get(i).getAsJsonObject();
            JsonObject source = propertySource.getAsJsonObject("source");

            if (source != null) {
                for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
                    String value = entry.getValue().isJsonPrimitive()
                            ? entry.getValue().getAsString()
                            : entry.getValue().toString();
                    cache.put(entry.getKey(), value);
                }
            }
        }
    }

    /**
     * Forces a refresh of the cached configuration.
     */
    public void refresh() {
        lastFetchTime = 0;
        ensureFetched();
    }

    /**
     * Builder for creating SpringCloudConfigSource instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serverUrl;
        private String application;
        private String profile = "default";
        private String label = "main";
        private String username;
        private String password;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
        private long cacheTimeoutMs = 30000;
        private int priority = 35;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;

        public Builder serverUrl(String url) {
            this.serverUrl = url;
            return this;
        }

        public Builder application(String application) {
            this.application = application;
            return this;
        }

        public Builder profile(String profile) {
            this.profile = profile;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder connectTimeoutMs(int timeout) {
            this.connectTimeoutMs = timeout;
            return this;
        }

        public Builder readTimeoutMs(int timeout) {
            this.readTimeoutMs = timeout;
            return this;
        }

        public Builder cacheTimeoutMs(long timeout) {
            this.cacheTimeoutMs = timeout;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder retryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
            return this;
        }

        public SpringCloudConfigSource build() {
            if (serverUrl == null || serverUrl.isEmpty()) {
                throw new IllegalArgumentException("Server URL is required");
            }
            if (application == null || application.isEmpty()) {
                throw new IllegalArgumentException("Application name is required");
            }
            return new SpringCloudConfigSource(serverUrl, application, profile, label,
                    username, password, connectTimeoutMs, readTimeoutMs, cacheTimeoutMs, priority,
                    maxRetries, retryDelayMs);
        }
    }
}
