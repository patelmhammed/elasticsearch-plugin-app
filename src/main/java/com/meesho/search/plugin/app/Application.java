package com.meesho.search.plugin.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot host application that loads Elasticsearch plugins via PF4J
 * and exposes REST endpoints for search and write operations.
 * <p>
 * The ES version is selected per-request via the URL path:
 * {@code POST /api/es/8.5/search} or {@code POST /api/es/8.13/search}.
 * <p>
 * Configuration (application.yml or system properties):
 * <ul>
 *   <li>{@code plugin.dir} — path to the directory containing plugin JARs (default: {@code plugins})</li>
 *   <li>{@code es.host}, {@code es.port}, {@code es.scheme} — ES connection settings
 *       (read by each plugin at startup)</li>
 * </ul>
 * <p>
 * Setup:
 * <pre>
 *   # 1. Build plugins:      cd plugins &amp;&amp; mvn clean install
 *   # 2. Create plugin dir:  mkdir -p es-plugins
 *   # 3. Copy plugin JARs:
 *   #      cp plugins/elasticsearch-plugin-impl-es85/target/elasticsearch-plugin-impl-es85-*.jar es-plugins/
 *   #      cp plugins/elasticsearch-plugin-impl-es813/target/elasticsearch-plugin-impl-es813-*.jar es-plugins/
 *   # 4. Build &amp; run app:   cd elasticsearch-plugin-app &amp;&amp; mvn clean package
 *   #      java -jar target/elasticsearch-plugin-app-1.0.0-SNAPSHOT.jar --plugin.dir=../es-plugins
 * </pre>
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
