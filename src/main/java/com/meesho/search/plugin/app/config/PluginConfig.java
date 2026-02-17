package com.meesho.search.plugin.app.config;

import com.meesho.search.plugin.api.model.ElasticsearchConnectionConfig;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configures the PF4J {@link PluginManager} and
 * {@link ElasticsearchConnectionConfig} as Spring beans.
 * <p>
 * The plugin directory is configurable via {@code plugin.dir} property
 * (defaults to {@code plugins} relative to the working directory).
 * Both ES plugin JARs (es85, es813) should be placed in this directory.
 * <p>
 * ES connection properties are read from {@code es.*} in application.yml
 * and injected into every plugin extension at registry startup.
 */
@Configuration
public class PluginConfig {

    private static final Logger log = LoggerFactory.getLogger(PluginConfig.class);

    private PluginManager pluginManager;

    @Bean
    public PluginManager pluginManager(@Value("${plugin.dir:plugins}") String pluginDir) {
        Path pluginsPath = Paths.get(pluginDir).toAbsolutePath();
        log.info("Loading PF4J plugins from: {}", pluginsPath);

        pluginManager = new DefaultPluginManager(pluginsPath);
        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        log.info("Loaded {} plugin(s)", pluginManager.getPlugins().size());
        pluginManager.getPlugins().forEach(pw ->
                log.info("  Plugin: id='{}', state={}", pw.getPluginId(), pw.getPluginState())
        );

        return pluginManager;
    }

    @Bean
    public ElasticsearchConnectionConfig elasticsearchConnectionConfig(
            @Value("${es.host:localhost}") String host,
            @Value("${es.port:9200}") int port,
            @Value("${es.scheme:http}") String scheme,
            @Value("${es.username:#{null}}") String username,
            @Value("${es.password:#{null}}") String password,
            @Value("${es.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${es.socket-timeout-ms:30000}") int socketTimeoutMs) {

        ElasticsearchConnectionConfig config = ElasticsearchConnectionConfig.builder()
                .host(host)
                .port(port)
                .scheme(scheme)
                .username(username)
                .password(password)
                .connectTimeoutMs(connectTimeoutMs)
                .socketTimeoutMs(socketTimeoutMs)
                .build();

        log.info("Elasticsearch connection config: {}://{}:{}", scheme, host, port);
        return config;
    }

    @PreDestroy
    public void stopPlugins() {
        if (pluginManager != null) {
            log.info("Stopping PF4J plugins");
            pluginManager.stopPlugins();
        }
    }
}
