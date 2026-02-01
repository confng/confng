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
 * Configuration source that fetches configuration from HashiCorp Consul KV store.
 *
 * <p>This source connects to a Consul agent and retrieves configuration values
 * from the key-value store under a specified prefix.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Simple usage with default localhost:8500
 * ConsulConfigSource source = new ConsulConfigSource("myapp/config");
 * ConfNG.addSource(source);
 * 
 * // With custom Consul address and ACL token
 * ConsulConfigSource source = ConsulConfigSource.builder()
 *     .host("consul.example.com")
 *     .port(8500)
 *     .prefix("myapp/config")
 *     .aclToken("your-acl-token")
 *     .build();
 * ConfNG.addSource(source);
 * }</pre>
 *
 * @author Bharat Kumar Malviya
 * @author GitHub: github.com/imBharatMalviya
 * @since 1.1.0
 */
@Slf4j
public class ConsulConfigSource implements ConfigSource {
    
    private final String host;
    private final int port;
    private final String prefix;
    private final String aclToken;
    private final boolean useSsl;
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
     * Creates a new Consul config source with default settings.
     *
     * @param prefix the key prefix to fetch (e.g., "myapp/config")
     */
    public ConsulConfigSource(String prefix) {
        this("localhost", 8500, prefix, null, false, 5000, 10000, 30000, 35, 3, 1000);
    }

    /**
     * Creates a new Consul config source with custom settings.
     */
    public ConsulConfigSource(String host, int port, String prefix, String aclToken,
                               boolean useSsl, int connectTimeoutMs, int readTimeoutMs,
                               long cacheTimeoutMs, int priority) {
        this(host, port, prefix, aclToken, useSsl, connectTimeoutMs, readTimeoutMs,
             cacheTimeoutMs, priority, 3, 1000);
    }

    /**
     * Creates a new Consul config source with custom settings including retry configuration.
     */
    public ConsulConfigSource(String host, int port, String prefix, String aclToken,
                               boolean useSsl, int connectTimeoutMs, int readTimeoutMs,
                               long cacheTimeoutMs, int priority, int maxRetries, long retryDelayMs) {
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or empty");
        }
        this.host = host;
        this.port = port;
        this.prefix = prefix.startsWith("/") ? prefix.substring(1) : prefix;
        this.aclToken = aclToken;
        this.useSsl = useSsl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.cacheTimeoutMs = cacheTimeoutMs;
        this.priority = priority;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }
    
    @Override
    public String getName() {
        return "Consul:" + host + ":" + port + "/" + prefix;
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
                    log.info("Retry attempt {} of {} for Consul {}", attempt, maxRetries, prefix);
                    Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                }
                fetchConfig();
                consecutiveFailures = 0;
                lastFetchTime = now;
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Fetch interrupted for Consul {}", prefix);
                break;
            } catch (Exception e) {
                lastException = e;
                log.warn("Fetch attempt {} failed for Consul {}: {}", attempt + 1, prefix, e.getMessage());
            }
        }

        consecutiveFailures++;
        lastFetchTime = now;
        log.error("Failed to fetch config from Consul {} after {} attempts", prefix, maxRetries + 1, lastException);
    }
    
    private void fetchConfig() throws Exception {
        String protocol = useSsl ? "https" : "http";
        String url = String.format("%s://%s:%d/v1/kv/%s?recurse=true", protocol, host, port, prefix);
        
        log.info("Fetching configuration from Consul: {}", url);
        
        URL consulUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) consulUrl.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            
            if (aclToken != null && !aclToken.isEmpty()) {
                conn.setRequestProperty("X-Consul-Token", aclToken);
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 404) {
                log.warn("Consul prefix not found: {}", prefix);
                return;
            }
            if (responseCode != 200) {
                throw new RuntimeException("Consul request failed with code: " + responseCode);
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            parseConsulResponse(response.toString());
            log.info("Successfully fetched {} config values from Consul", cache.size());
        } finally {
            conn.disconnect();
        }
    }
    
    private void parseConsulResponse(String json) {
        cache.clear();
        JsonArray entries = gson.fromJson(json, JsonArray.class);

        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            String fullKey = entry.get("Key").getAsString();
            JsonElement valueElement = entry.get("Value");

            if (valueElement == null || valueElement.isJsonNull()) {
                continue;
            }

            // Decode Base64 value
            String base64Value = valueElement.getAsString();
            String value = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);

            // Remove prefix from key and convert slashes to dots
            String key = fullKey;
            if (key.startsWith(prefix + "/")) {
                key = key.substring(prefix.length() + 1);
            } else if (key.startsWith(prefix)) {
                key = key.substring(prefix.length());
            }
            key = key.replace("/", ".");

            if (!key.isEmpty()) {
                cache.put(key, value);
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
     * Builder for creating ConsulConfigSource instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String host = "localhost";
        private int port = 8500;
        private String prefix;
        private String aclToken;
        private boolean useSsl = false;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
        private long cacheTimeoutMs = 30000;
        private int priority = 35;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder aclToken(String token) {
            this.aclToken = token;
            return this;
        }

        public Builder useSsl(boolean useSsl) {
            this.useSsl = useSsl;
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

        public ConsulConfigSource build() {
            if (prefix == null || prefix.isEmpty()) {
                throw new IllegalArgumentException("Prefix is required");
            }
            return new ConsulConfigSource(host, port, prefix, aclToken, useSsl,
                    connectTimeoutMs, readTimeoutMs, cacheTimeoutMs, priority, maxRetries, retryDelayMs);
        }
    }
}

