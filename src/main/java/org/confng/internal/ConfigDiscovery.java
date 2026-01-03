package org.confng.internal;

import org.confng.api.ConfNGKey;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Internal helper class responsible for discovering configuration keys.
 * This class uses reflection to find all implementations of {@link ConfNGKey}
 * in the classpath.
 *
 * <p>This is an internal class and should not be used directly by clients.
 * Use {@link org.confng.ConfNG} instead.</p>
 *
 * @author Bharat Kumar Malviya
 * @since 1.0
 */
public final class ConfigDiscovery {

    private ConfigDiscovery() {
        // Utility class - prevent instantiation
    }

    /**
     * Discovers all ConfNGKey implementations in the classpath.
     *
     * @param basePackages packages to scan, if empty scans entire classpath
     * @return list of discovered configuration keys
     */
    public static List<ConfNGKey> discoverAllConfigKeys(String... basePackages) {
        List<ConfNGKey> discovered = new ArrayList<>();
        List<String> packages = (basePackages == null || basePackages.length == 0)
                ? Arrays.asList("")
                : Arrays.asList(basePackages);

        for (String p : packages) {
            Reflections reflections = (p == null || p.isEmpty()) ? new Reflections() : new Reflections(p);
            Set<Class<? extends ConfNGKey>> subtypes = reflections.getSubTypesOf(ConfNGKey.class);

            for (Class<? extends ConfNGKey> cls : subtypes) {
                if (cls.isEnum()) {
                    Object[] constants = cls.getEnumConstants();
                    if (constants != null) {
                        for (Object c : constants) {
                            discovered.add((ConfNGKey) c);
                        }
                    }
                }
            }
        }
        return discovered;
    }
}

