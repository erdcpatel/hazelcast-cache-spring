package com.example.hazelcast.controller;

import com.hazelcast.core.HazelcastInstance;
import com.example.hazelcast.config.HazelcastConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @BeforeEach
    public void setup() {
        hazelcastInstance.getMap("default").clear();
    }

    @Test
    public void testPutEntry() throws Exception {
        mockMvc.perform(put("/cache/testKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content("testValue"))
                .andExpect(status().isOk())
                .andExpect(content().string("Entry added to cache."));
    }

    @Test
    public void testGetEntry() throws Exception {
        hazelcastInstance.getMap("default").put("testKey", "testValue");

        mockMvc.perform(get("/cache/testKey"))
                .andExpect(status().isOk())
                .andExpect(content().string("testValue"));
    }

    @Test
    public void testDeleteEntry() throws Exception {
        hazelcastInstance.getMap("default").put("testKey", "testValue");

        mockMvc.perform(delete("/cache/testKey"))
                .andExpect(status().isOk())
                .andExpect(content().string("Entry removed from cache."));
    }

    @Test
    public void testGetCacheStats() throws Exception {
        hazelcastInstance.getMap("default").put("testKey", "testValue");

        mockMvc.perform(get("/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.isEmpty").value(false));
    }

    @Test
    public void testQueryCache() throws Exception {
        hazelcastInstance.getMap("default").put("testKey1", "value1");
        hazelcastInstance.getMap("default").put("testKey2", "value2");

        mockMvc.perform(get("/cache/query").param("keyPrefix", "testKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testKey1").value("value1"))
                .andExpect(jsonPath("$.testKey2").value("value2"));
    }

    @Test
    public void testLockAndUnlockKey() throws Exception {
        mockMvc.perform(post("/cache/lock/testKey"))
                .andExpect(status().isOk())
                .andExpect(content().string("Key locked: testKey"));

        mockMvc.perform(post("/cache/unlock/testKey"))
                .andExpect(status().isOk())
                .andExpect(content().string("Key unlocked: testKey"));
    }

    @Test
    public void testAddListeners() throws Exception {
        mockMvc.perform(post("/cache/listener"))
                .andExpect(status().isOk())
                .andExpect(content().string("Listeners added to cache."));
    }

    @Test
    public void testUnlockKeyErrorHandling() throws Exception {
        // Attempt to unlock a key that is not locked
        mockMvc.perform(post("/cache/unlock/unlockedKey"))
                .andExpect(status().isOk())
                .andExpect(content().string("Key is not locked: unlockedKey"));

        // Lock and then unlock the key
        mockMvc.perform(post("/cache/lock/testKey"))
                .andExpect(status().isOk())
                .andExpect(content().string("Key locked: testKey"));

        mockMvc.perform(post("/cache/unlock/testKey"))
                .andExpect(status().isOk())
                .andExpect(content().string("Key unlocked: testKey"));
    }
}