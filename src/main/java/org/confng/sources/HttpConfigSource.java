package org.confng.sources;

import com.google.gson.Gson;
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
 * Configuration source that fetches configuration from HTTP/REST endpoints.
 *
 * <p>This source supports fetching configuration from any HTTP endpoint that returns
 * JSON data. It includes caching, authentication, and retry support.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Simple usage
 * HttpConfigSource source = new HttpConfigSource("https://config.example.com/api/config");
 * ConfNG.addSource(source);
 * 
 * // With authentication
 * HttpConfigSource source = HttpConfigSource.builder()
 *     .url("https://config.example.com/api/config")
 *     .bearerToken("your-token")
 *     .cacheTimeoutMs(60000)
 *     .build();
 * ConfNG.addSource(source);
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Slf4j
public class HttpConfigSource implements ConfigSource {
    
    private final String url;
    private final Map<String, String> headers;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final long cacheTimeoutMs;
    private final int priority;
    private final int maxRetries;
    private final long retryDelayMs;
    private final Gson gson = new Gson();

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile long lastFetchTime = 0;
    private volatile boolean fetchFailed = false;
    private volatile int consecutiveFailures = 0;
    
    /**
     * Creates a new HTTP config source with default settings.
     *
     * @param url the URL to fetch configuration from
     */
    public HttpConfigSource(String url) {
        this(url, Collections.emptyMap(), 5000, 10000, 60000, 30, 3, 1000);
    }

    /**
     * Creates a new HTTP config source with custom settings.
     */
    public HttpConfigSource(String url, Map<String, String> headers,
                            int connectTimeoutMs, int readTimeoutMs,
                            long cacheTimeoutMs, int priority) {
        this(url, headers, connectTimeoutMs, readTimeoutMs, cacheTimeoutMs, priority, 3, 1000);
    }

    /**
     * Creates a new HTTP config source with custom settings including retry configuration.
     */
    public HttpConfigSource(String url, Map<String, String> headers,
                            int connectTimeoutMs, int readTimeoutMs,
                            long cacheTimeoutMs, int priority,
                            int maxRetries, long retryDelayMs) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        this.url = url;
        this.headers = new HashMap<>(headers);
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.cacheTimeoutMs = cacheTimeoutMs;
        this.priority = priority;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }
    
    @Override
    public String getName() {
        return "HTTP:" + url;
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
                    log.info("Retry attempt {} of {} for {}", attempt, maxRetries, url);
                    Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                }
                fetchConfig();
                fetchFailed = false;
                consecutiveFailures = 0;
                lastFetchTime = now;
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Fetch interrupted for {}", url);
                break;
            } catch (Exception e) {
                lastException = e;
                log.warn("Fetch attempt {} failed for {}: {}", attempt + 1, url, e.getMessage());
            }
        }

        consecutiveFailures++;
        fetchFailed = true;
        lastFetchTime = now;
        log.error("Failed to fetch config from {} after {} attempts", url, maxRetries + 1, lastException);
    }
    
    private void fetchConfig() throws Exception {
        log.info("Fetching configuration from {}", url);
        
        URL configUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) configUrl.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Accept", "application/json");
            
            for (Map.Entry<String, String> header : headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP request failed with code: " + responseCode);
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            parseAndCacheConfig(response.toString());
            log.info("Successfully fetched {} config values from {}", cache.size(), url);
        } finally {
            conn.disconnect();
        }
    }
    
    private void parseAndCacheConfig(String json) {
        cache.clear();
        JsonObject root = gson.fromJson(json, JsonObject.class);
        flattenJson("", root, cache);
    }
    
    private void flattenJson(String prefix, JsonObject obj, Map<String, String> result) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonElement value = entry.getValue();
            
            if (value.isJsonObject()) {
                flattenJson(key, value.getAsJsonObject(), result);
            } else if (value.isJsonPrimitive()) {
                result.put(key, value.getAsString());
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
     * Checks if the last fetch failed.
     */
    public boolean isFetchFailed() {
        return fetchFailed;
    }

    /**
     * Builder for creating HttpConfigSource instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private Map<String, String> headers = new HashMap<>();
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
        private long cacheTimeoutMs = 60000;
        private int priority = 30;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder bearerToken(String token) {
            this.headers.put("Authorization", "Bearer " + token);
            return this;
        }

        public Builder basicAuth(String username, String password) {
            String credentials = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            this.headers.put("Authorization", "Basic " + encoded);
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

        public HttpConfigSource build() {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("URL is required");
            }
            return new HttpConfigSource(url, headers, connectTimeoutMs, readTimeoutMs, cacheTimeoutMs, priority);
        }
    }
}

