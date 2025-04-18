package com.example.hazelcast.controller;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.OverflowPolicy;
import com.hazelcast.ringbuffer.ReadResultSet;
import com.hazelcast.ringbuffer.Ringbuffer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ringbuffer")
@Tag(name = "Hazelcast Ringbuffer Controller", description = "APIs for interacting with the Hazelcast Ringbuffer")
public class RingbufferController {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private static final Logger logger = LoggerFactory.getLogger(RingbufferController.class);

    private Ringbuffer<String> getRingbuffer() {
        // Assuming String items for simplicity, adjust if needed
        return hazelcastInstance.getRingbuffer("ringbuffer-demo");
    }

    @PostMapping("/add")
    @Operation(summary = "Add an item to the ringbuffer", description = "Adds the given item to the end of the ringbuffer. Returns the sequence number assigned to the item. If the buffer is full, the oldest item might be overwritten.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added successfully, returns sequence number",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during add operation")
    })
    public CompletionStage<ResponseEntity<Long>> addItem(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The item (String) to add to the ringbuffer", required = true,
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class)))
            @RequestBody String item) {
        logger.info("Adding item to ringbuffer: {}", item);
        return getRingbuffer().addAsync(item, OverflowPolicy.OVERWRITE)
                .thenApply(seq -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(seq));
    }

    @GetMapping("/read")
    @Operation(summary = "Read items from the ringbuffer", description = "Reads up to 10 items starting from the specified sequence number (inclusive). If the sequence number is older than the head sequence, it starts reading from the head.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Items read successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during read operation")
    })
    public CompletionStage<ResponseEntity<List<String>>> readItems(
            @Parameter(description = "The sequence number to start reading from (inclusive)", required = true) @RequestParam long startSequence) {
        logger.info("Reading items from ringbuffer starting at sequence: {}", startSequence);
        Ringbuffer<String> ringbuffer = getRingbuffer();
        // Read up to 10 items, or fewer if less are available
        return ringbuffer.readManyAsync(startSequence, 1, 10, null)
                .thenApply(resultSet -> {
                    List<String> result = new java.util.ArrayList<>();
                    for (int i = 0; i < resultSet.readCount(); i++) {
                        result.add(resultSet.get(i));
                    }
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
                });
    }

    @GetMapping("/info")
    @Operation(summary = "Get ringbuffer information", description = "Retrieves information about the ringbuffer like capacity, size, head and tail sequences.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ringbuffer info retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class)))
    })
    public Map<String, Object> getRingbufferInfo() {
        Ringbuffer<String> ringbuffer = getRingbuffer();
        logger.info("Fetching ringbuffer info: capacity={}, size={}, head={}, tail={}",
                ringbuffer.capacity(), ringbuffer.size(), ringbuffer.headSequence(), ringbuffer.tailSequence());
        return Map.of(
                "name", ringbuffer.getName(),
                "capacity", ringbuffer.capacity(),
                "size", ringbuffer.size(),
                "remainingCapacity", ringbuffer.remainingCapacity(),
                "headSequence", ringbuffer.headSequence(), // Oldest item sequence
                "tailSequence", ringbuffer.tailSequence()  // Newest item sequence
        );
    }
}
