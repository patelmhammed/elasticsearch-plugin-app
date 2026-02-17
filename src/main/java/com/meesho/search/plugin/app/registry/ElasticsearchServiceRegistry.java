package com.meesho.search.plugin.app.registry;

import com.meesho.search.plugin.api.ConnectionService;
import com.meesho.search.plugin.api.SearchService;
import com.meesho.search.plugin.api.WriteService;
import com.meesho.search.plugin.api.model.ElasticsearchConnectionConfig;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry that discovers all PF4J extensions at startup and indexes them by ES version.
 * <p>
 * Startup order:
 * <ol>
 *   <li>Discover {@link ConnectionService} extensions and call {@code connect(config)}
 *       to initialize the ES client in each plugin.</li>
 *   <li>Discover {@link SearchService} and {@link WriteService} extensions (already
 *       backed by an initialized client).</li>
 * </ol>
 * <p>
 * Version-to-plugin mapping:
 * <ul>
 *   <li>{@code 8.5}  -> {@code elasticsearch-es85-plugin}</li>
 *   <li>{@code 8.13} -> {@code elasticsearch-es813-plugin}</li>
 * </ul>
 */
@Service
public class ElasticsearchServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchServiceRegistry.class);

    /** Maps user-facing version string to PF4J plugin ID. */
    private static final Map<String, String> VERSION_TO_PLUGIN_ID = Map.of(
            "8.5",  "elasticsearch-es85-plugin",
            "8.13", "elasticsearch-es813-plugin",
            "9.1",  "elasticsearch-es91-plugin"
    );

    private final Map<String, SearchService> searchServices;
    private final Map<String, WriteService> writeServices;

    public ElasticsearchServiceRegistry(PluginManager pluginManager,
                                        ElasticsearchConnectionConfig esConfig) {
        // Step 1: discover ConnectionService extensions and establish connections
        connectPlugins(pluginManager, esConfig);

        // Step 2: discover Search/Write extensions (clients already initialized)
        this.searchServices = discoverExtensions(pluginManager, SearchService.class);
        this.writeServices = discoverExtensions(pluginManager, WriteService.class);

        log.info("ElasticsearchServiceRegistry initialized — available versions: search={}, write={}",
                searchServices.keySet(), writeServices.keySet());
    }

    /**
     * Returns the {@link SearchService} for the given ES version.
     *
     * @param version ES version string (e.g. "8.5", "8.13")
     * @throws IllegalArgumentException if the version is not available
     */
    public SearchService getSearchService(String version) {
        SearchService service = searchServices.get(version);
        if (service == null) {
            throw new IllegalArgumentException(
                    "No SearchService available for ES version '" + version
                            + "'. Available: " + searchServices.keySet());
        }
        return service;
    }

    /**
     * Returns the {@link WriteService} for the given ES version.
     *
     * @param version ES version string (e.g. "8.5", "8.13")
     * @throws IllegalArgumentException if the version is not available
     */
    public WriteService getWriteService(String version) {
        WriteService service = writeServices.get(version);
        if (service == null) {
            throw new IllegalArgumentException(
                    "No WriteService available for ES version '" + version
                            + "'. Available: " + writeServices.keySet());
        }
        return service;
    }

    /**
     * @return the set of ES versions that have both SearchService and WriteService available
     */
    public Set<String> getAvailableVersions() {
        return Collections.unmodifiableSet(searchServices.keySet());
    }

    // ---- internal helpers ----

    /**
     * Discovers all {@link ConnectionService} extensions, resolves each to an ES version,
     * and calls {@code connect(config)} to initialize the ES client for that plugin.
     */
    private void connectPlugins(PluginManager pluginManager, ElasticsearchConnectionConfig esConfig) {
        for (ConnectionService connSvc : pluginManager.getExtensions(ConnectionService.class)) {
            PluginWrapper wrapper = pluginManager.whichPlugin(connSvc.getClass());
            if (wrapper == null) {
                log.warn("ConnectionService {} has no owning plugin, skipping",
                        connSvc.getClass().getName());
                continue;
            }

            String pluginId = wrapper.getPluginId();
            String version = resolveVersion(pluginId);

            if (version == null) {
                log.warn("Plugin '{}' is not mapped to any ES version, skipping ConnectionService {}",
                        pluginId, connSvc.getClass().getName());
                continue;
            }

            try {
                connSvc.connect(esConfig);
                log.info("Connected ES client for version {} (plugin '{}')", version, pluginId);
            } catch (Exception e) {
                log.error("Failed to connect ES client for version {} (plugin '{}'): {}",
                        version, pluginId, e.getMessage(), e);
                throw new RuntimeException("Plugin connection failed for ES " + version, e);
            }
        }
    }

    /**
     * Discovers all extensions of the given type and associates each with its ES version
     * via {@link PluginManager#whichPlugin(Class)}.
     */
    private <T> Map<String, T> discoverExtensions(PluginManager pluginManager, Class<T> type) {
        Map<String, T> result = new LinkedHashMap<>();

        for (T extension : pluginManager.getExtensions(type)) {
            PluginWrapper wrapper = pluginManager.whichPlugin(extension.getClass());
            if (wrapper == null) {
                log.warn("Extension {} has no owning plugin (loaded from system classloader), skipping",
                        extension.getClass().getName());
                continue;
            }

            String pluginId = wrapper.getPluginId();
            String version = resolveVersion(pluginId);

            if (version == null) {
                log.warn("Plugin '{}' is not mapped to any ES version, skipping extension {}",
                        pluginId, extension.getClass().getName());
                continue;
            }

            result.put(version, extension);
            log.info("Registered {} [version={}] from plugin '{}'",
                    type.getSimpleName(), version, pluginId);
        }

        return result;
    }

    /**
     * Reverse-lookup: returns the user-facing version string for a given PF4J plugin ID,
     * or {@code null} if not mapped.
     */
    private String resolveVersion(String pluginId) {
        return VERSION_TO_PLUGIN_ID.entrySet().stream()
                .filter(e -> e.getValue().equals(pluginId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
