# Elasticsearch Plugin App

A Spring Boot app that loads Elasticsearch plugins (via PF4J) and exposes REST APIs for **search**, **index**, and **bulk-index**. You pick the Elasticsearch version in the URL.

## Prerequisites

- Java 17
- Maven 3.8+
- The [elasticsearch-pf4j-plugin](https://github.com/patelmhammed/elasticsearch-pf4j-plugin) repo (to build the plugin JARs)

## How to use

### 1. Build the plugins

From the **elasticsearch-pf4j-plugin** repo:

```bash
cd elasticsearch-pf4j-plugin
mvn clean package
```

### 2. Create a plugin folder and copy JARs

```bash
mkdir -p es-plugins
cp elasticsearch-pf4j-plugin/elasticsearch-plugin-impl-es85/target/elasticsearch-plugin-impl-es85-*.jar es-plugins/
cp elasticsearch-pf4j-plugin/elasticsearch-plugin-impl-es813/target/elasticsearch-plugin-impl-es813-*.jar es-plugins/
cp elasticsearch-pf4j-plugin/elasticsearch-plugin-impl-es91/target/elasticsearch-plugin-impl-es91-*.jar es-plugins/
```

Copy only the JARs you need (e.g. skip es91 if you don’t use it).

### 3. Build and run this app

```bash
cd elasticsearch-plugin-app
mvn clean package
java -jar target/elasticsearch-plugin-app-1.0.0-SNAPSHOT.jar --plugin.dir=/path/to/es-plugins
```

Or point to your plugin dir:

```bash
java -jar target/elasticsearch-plugin-app-1.0.0-SNAPSHOT.jar --plugin.dir=../es-plugins
```

### 4. Call the APIs

Base path: **`/api/es/{version}`**  
Use version **8.5**, **8.13**, or **9.1** depending on which plugin JARs you put in `plugin.dir`.

| What            | Method | Path                    |
|-----------------|--------|-------------------------|
| List versions  | GET    | `/api/es/8.5/versions`   |
| Search          | POST   | `/api/es/8.5/search`     |
| Index one doc   | POST   | `/api/es/8.5/index`     |
| Bulk index      | POST   | `/api/es/8.5/bulk-index` |

**Example — search**

```bash
curl -X POST http://localhost:8080/api/es/8.5/search \
  -H "Content-Type: application/json" \
  -d '{"indexName":"my_index","query":{},"from":0,"size":10}'
```

**Example — index one document**

```bash
curl -X POST http://localhost:8080/api/es/8.5/index \
  -H "Content-Type: application/json" \
  -d '{"indexName":"my_index","id":"1","source":{"title":"Hello","count":1}}'
```

## Configuration

- **plugin.dir** — folder containing the plugin JARs (default: `plugins`).
- **es.host**, **es.port**, **es.scheme** — Elasticsearch connection (default: `localhost`, `9200`, `http`).  
  You can set these in `application.yml` or as system properties / command-line args.

## Summary

1. Build plugin JARs in **elasticsearch-pf4j-plugin**.
2. Put those JARs in a directory and pass it as **plugin.dir**.
3. Run this app and call **/api/es/{version}/search**, **/index**, **/bulk-index** as needed.
