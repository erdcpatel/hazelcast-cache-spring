package com.example.hazelcast.controller;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.OverflowPolicy;
import com.hazelcast.ringbuffer.ReadResultSet;
import com.hazelcast.ringbuffer.Ringbuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RingbufferController.class)
public class RingbufferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HazelcastInstance hazelcastInstance;

    @MockBean(name = "ringbuffer-demo") // Mock the specific Ringbuffer bean
    private Ringbuffer<String> ringbuffer;

    @BeforeEach
    void setUp() {
        // Ensure the mock HazelcastInstance returns the mock Ringbuffer
        when(hazelcastInstance.<String>getRingbuffer(eq("ringbuffer-demo"))).thenReturn(ringbuffer);
    }

    @Test
    void addItem() throws Exception {
        String item = "event1";
        long expectedSequence = 10L;
        CompletableFuture<Long> future = CompletableFuture.completedFuture(expectedSequence);

        when(ringbuffer.addAsync(eq(item), eq(OverflowPolicy.OVERWRITE))).thenReturn(future);

        var mvcResult = mockMvc.perform(post("/ringbuffer/add")
                .contentType(MediaType.TEXT_PLAIN)
                .content(item))
                .andExpect(status().isOk())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(expectedSequence));

        verify(ringbuffer).addAsync(eq(item), eq(OverflowPolicy.OVERWRITE));
    }

    @Test
    void readItems() throws Exception {
        long startSequence = 5L;
        List<String> items = Arrays.asList("item5", "item6", "item7");

        // Stub ReadResultSet
        ReadResultSet<String> resultSet = new ReadResultSet<>() {
            @Override public int readCount() { return items.size(); }
            @Override public String get(int index) { return items.get(index); }
            @Override public long getNextSequenceToReadFrom() { return 0L; }
            @Override public int size() { return items.size(); }
            @Override public long getSequence(int index) { return index; }
            @Override public java.util.Iterator<String> iterator() { return items.iterator(); }
            // ...other methods can throw UnsupportedOperationException if called...
        };
        CompletableFuture<ReadResultSet<String>> future = CompletableFuture.completedFuture(resultSet);
        when(ringbuffer.readManyAsync(eq(startSequence), eq(1), eq(10), isNull())).thenReturn(future);

        var mvcResult2 = mockMvc.perform(get("/ringbuffer/read")
                .param("startSequence", String.valueOf(startSequence)))
                .andExpect(status().isOk())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult2))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(items.size()))
                .andExpect(jsonPath("$[0]").value(items.get(0)))
                .andExpect(jsonPath("$[1]").value(items.get(1)))
                .andExpect(jsonPath("$[2]").value(items.get(2)));

        verify(ringbuffer).readManyAsync(eq(startSequence), eq(1), eq(10), ArgumentMatchers.isNull());
    }

    @Test
    void getRingbufferInfo() throws Exception {
        long capacity = 100L;
        long size = 15L;
        long head = 0L;
        long tail = 14L;
        String name = "ringbuffer-demo";

        when(ringbuffer.getName()).thenReturn(name);
        when(ringbuffer.capacity()).thenReturn(capacity);
        when(ringbuffer.size()).thenReturn(size);
        when(ringbuffer.remainingCapacity()).thenReturn(capacity - size);
        when(ringbuffer.headSequence()).thenReturn(head);
        when(ringbuffer.tailSequence()).thenReturn(tail);

        mockMvc.perform(get("/ringbuffer/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.capacity").value(capacity))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.remainingCapacity").value(capacity - size))
                .andExpect(jsonPath("$.headSequence").value(head))
                .andExpect(jsonPath("$.tailSequence").value(tail));
    }
}
