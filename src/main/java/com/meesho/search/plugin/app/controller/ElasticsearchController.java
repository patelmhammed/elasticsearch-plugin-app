package com.meesho.search.plugin.app.controller;

import com.meesho.search.plugin.api.model.BulkIndexRequest;
import com.meesho.search.plugin.api.model.BulkIndexResponse;
import com.meesho.search.plugin.api.model.IndexRequest;
import com.meesho.search.plugin.api.model.IndexResponse;
import com.meesho.search.plugin.api.model.SearchRequest;
import com.meesho.search.plugin.api.model.SearchResponse;
import com.meesho.search.plugin.app.registry.ElasticsearchServiceRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * REST controller for Elasticsearch operations.
 * The ES version is specified per-request via the {@code {version}} path variable.
 * <p>
 * Example: {@code POST /api/es/8.13/search} uses the ES 8.13 plugin implementation.
 */
@RestController
@RequestMapping("/api/es/{version}")
public class ElasticsearchController {

    private final ElasticsearchServiceRegistry registry;

    public ElasticsearchController(ElasticsearchServiceRegistry registry) {
        this.registry = registry;
    }

    /**
     * Lists available ES versions.
     */
    @GetMapping("/versions")
    public ResponseEntity<Map<String, Set<String>>> getVersions() {
        return ResponseEntity.ok(Map.of("availableVersions", registry.getAvailableVersions()));
    }

    /**
     * Search documents.
     */
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @PathVariable String version,
            @RequestBody SearchRequest request) {
        SearchResponse response = registry.getSearchService(version).search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Index a single document.
     */
    @PostMapping("/index")
    public ResponseEntity<IndexResponse> index(
            @PathVariable String version,
            @RequestBody IndexRequest request) {
        IndexResponse response = registry.getWriteService(version).index(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk index multiple documents.
     */
    @PostMapping("/bulk-index")
    public ResponseEntity<BulkIndexResponse> bulkIndex(
            @PathVariable String version,
            @RequestBody BulkIndexRequest request) {
        BulkIndexResponse response = registry.getWriteService(version).bulkIndex(request);
        return ResponseEntity.ok(response);
    }
}
