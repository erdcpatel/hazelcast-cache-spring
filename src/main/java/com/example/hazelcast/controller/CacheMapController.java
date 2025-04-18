package com.example.hazelcast.controller;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicates;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cache")
@Tag(name = "Hazelcast Map Controller", description = "APIs for interacting with the Hazelcast distributed map (IMap)")
public class CacheMapController {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private static final Logger logger = LoggerFactory.getLogger(CacheMapController.class);

    private IMap<String, String> getCache() {
        return hazelcastInstance.getMap("default");
    }

    @PutMapping("/{key}")
    @Operation(summary = "Add or update an entry in the cache", description = "Stores the given value associated with the specified key in the Hazelcast 'default' map.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entry successfully added or updated",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during cache operation")
    })
    public String putEntry(
            @Parameter(description = "The key for the cache entry") @PathVariable String key,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The value to store in the cache", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = String.class)))
            @RequestBody String value) {
        logger.info("Adding entry to cache: key={}, value={}", key, value);
        try {
            getCache().put(key, value);
            return "Entry added to cache.";
        } catch (Exception e) {
            logger.error("Error adding entry to cache: key={}, value={}, error={}", key, value, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{key}")
    @Operation(summary = "Retrieve an entry from the cache", description = "Fetches the value associated with the specified key from the Hazelcast 'default' map.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entry found and returned or 'Entry not found.' message",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class)))
    })
    public String getEntry(
            @Parameter(description = "The key of the cache entry to retrieve") @PathVariable String key) {
        logger.info("Retrieving entry from cache: key={}", key);
        return getCache().getOrDefault(key, "Entry not found.");
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete an entry from the cache", description = "Removes the entry associated with the specified key from the Hazelcast 'default' map.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entry successfully removed",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class)))
    })
    public String deleteEntry(
            @Parameter(description = "The key of the cache entry to delete") @PathVariable String key) {
        logger.info("Removing entry from cache: key={}", key);
        getCache().remove(key);
        return "Entry removed from cache.";
    }

    @GetMapping("/stats")
    @Operation(summary = "Get cache statistics", description = "Retrieves statistics about the Hazelcast 'default' map, such as size and emptiness.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class)))
    })
    public Map<String, Object> getCacheStats() {
        IMap<String, String> cache = getCache();
        logger.info("Fetching cache statistics: size={}, isEmpty={}", cache.size(), cache.isEmpty());
        return Map.of(
                "size", cache.size(),
                "isEmpty", cache.isEmpty()
        );
    }

    @GetMapping("/query")
    @Operation(summary = "Query cache entries by key prefix", description = "Finds all entries in the Hazelcast 'default' map whose keys start with the specified prefix using a SQL-like predicate.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Query executed successfully, returns matching entries",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)))
    })
    public Map<String, String> queryCache(
            @Parameter(description = "The prefix to match keys against", required = true) @RequestParam String keyPrefix) {
        logger.info("Querying cache for keys starting with: {}", keyPrefix);
        // Use __key to query based on the map entry key
        return getCache().entrySet(Predicates.sql("__key LIKE '" + keyPrefix + "%'"))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @PostMapping("/lock/{key}")
    @Operation(summary = "Lock a cache key", description = "Acquires a distributed lock on the specified key in the Hazelcast 'default' map. Blocks until the lock is acquired.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key successfully locked",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class)))
    })
    public String lockKey(
            @Parameter(description = "The key to lock") @PathVariable String key) {
        logger.info("Locking key in cache: {}", key);
        getCache().lock(key);
        return "Key locked: " + key;
    }

    @PostMapping("/unlock/{key}")
    @Operation(summary = "Unlock a cache key", description = "Releases the distributed lock on the specified key in the Hazelcast 'default' map. Only effective if the key is currently locked by the calling thread/member.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key successfully unlocked or message indicating it wasn't locked",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during unlock operation")
    })
    public String unlockKey(
            @Parameter(description = "The key to unlock") @PathVariable String key) {
        logger.info("Unlocking key in cache: {}", key);
        try {
            if (getCache().isLocked(key)) {
                getCache().unlock(key);
                return "Key unlocked: " + key;
            } else {
                return "Key is not locked: " + key;
            }
        } catch (Exception e) {
            logger.error("Error unlocking key in cache: key={}, error={}", key, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/listener")
    @Operation(summary = "Add entry listeners to the cache", description = "Adds listeners to the Hazelcast 'default' map to log when entries are added or removed. This is primarily for demonstration.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listeners successfully added",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class)))
    })
    public String addListeners() {
        logger.info("Adding listeners to cache.");
        getCache().addEntryListener((EntryAddedListener<String, String>) event ->
                logger.info("Entry added: key={}, value={}", event.getKey(), event.getValue()), true);
        getCache().addEntryListener((EntryRemovedListener<String, String>) event ->
                logger.info("Entry removed: key={}, oldValue={}", event.getKey(), event.getOldValue()), true);
        return "Listeners added to cache.";
    }
}