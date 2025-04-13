# Hazelcast Cache Internals Notes

This document explains the internal workings of Hazelcast, particularly in the context of the `hazelcast-cache-spring` application deployed across multiple pods (e.g., 3 pods in Kubernetes).

## Core Concepts

### 1. Cluster Membership & Discovery

*   **Formation:** When Spring Boot applications start, the embedded Hazelcast instances discover each other to form a single, unified cluster.
*   **Discovery Mechanism:** Discovery typically relies on mechanisms configured in `HazelcastConfig.java` or `hazelcast.xml`. Common methods include:
    *   **Multicast:** Default, but often disabled in cloud/container environments.
    *   **TCP/IP:** Manually listing potential member addresses.
    *   **Kubernetes Discovery:** Automatically discovers other Hazelcast pods using the Kubernetes API (likely used in this project).
*   **Membership:** All participating pods become **members** of the Hazelcast cluster.

### 2. Data Partitioning

Hazelcast is an In-Memory Data Grid (IMDG) that partitions data across cluster members for scalability and resilience.

*   **Partitions:** The entire keyspace of a distributed data structure (like an `IMap`) is divided into a fixed number of partitions (default: 271).
*   **Partition Ownership:**
    *   **Primary Owner:** Hazelcast assigns primary ownership of each partition to a specific cluster member. In a 3-pod setup, each pod will be the primary owner for roughly 1/3rd of the partitions. The primary owner holds the authoritative copy of the data for its partitions.
    *   **Backup Owner(s):** For fault tolerance, Hazelcast assigns one or more backup owners for each partition.
        *   **Synchronous Backups (Default):** By default, each partition has one synchronous backup. Operations like `put` or `remove` are only considered complete after the primary *and* the synchronous backup confirm the change. This ensures no data loss if the primary fails.
        *   **Asynchronous Backups:** Can be configured for higher throughput at the cost of potential data loss during failures if the backup hasn't received the update yet.
*   **Partition Table:** Every member maintains a **partition table**, mapping each `partitionId` to its current primary and backup owners. This table is updated dynamically as the cluster topology changes.

## Endpoint Internals

### 1. `PUT` Operation (`/cache/{key}` with PUT)

When a `PUT` request hits *any* pod:

1.  **Request Reception:** The `CacheController` receives the HTTP request.
2.  **Get Map Proxy:** `hazelcastInstance.getMap("default")` provides a smart proxy to the distributed map. It doesn't hold the data itself but knows how to interact with the cluster.
3.  **`cache.put(key, value)` Execution:**
    *   **Hashing:** Hazelcast calculates the hash of the `key`.
    *   **Partition ID Calculation:** The hash determines the target `partitionId` (`hash(key) % numberOfPartitions`).
    *   **Partition Owner Lookup:** The instance consults its local partition table to find the **primary owner** pod for that `partitionId`.
    *   **Request Routing:**
        *   If the current pod *is* the primary owner, it processes the request locally.
        *   If not, it forwards the `put` request internally (via network) to the correct primary owner pod.
    *   **Primary Storage:** The primary owner pod stores the `key`/`value` in its local memory segment for that partition.
    *   **Synchronous Backup Replication:** The primary sends the `put` operation to the designated synchronous backup pod(s).
    *   **Acknowledgement:** The primary waits for confirmation from the backup(s) that they have successfully stored the data copy.
    *   **Response:** Only after the primary stores the data *and* receives acknowledgement from synchronous backup(s) is the operation successful. A success response is sent back (potentially routed back to the original receiving pod) and then returned as the HTTP 200 response.

### 2. `DELETE` Operation (`/cache/{key}` with DELETE)

The process for `cache.remove(key)` is analogous to `put`:

1.  Request Reception, Get Map Proxy.
2.  `cache.remove(key)` Execution:
    *   Hashing, Partition ID Calculation, Partition Owner Lookup.
    *   Request Routing (if needed) to the primary owner.
    *   **Primary Deletion:** The primary owner removes the entry from its local memory.
    *   **Synchronous Backup Deletion:** The primary sends the `remove` operation to the synchronous backup(s).
    *   **Acknowledgement:** The primary waits for confirmation from the backup(s).
    *   **Response:** Once primary and backup(s) confirm deletion, a success response is sent back.

## Data Synchronization & Consistency

*   **Mechanism:** Synchronization is achieved via the **partitioning** and **primary-backup** model, not full replication.
*   **Consistency (with Sync Backups):** Using the default synchronous backups provides **strong consistency**. A read operation following a successful write operation is guaranteed to see the written value, even if the primary owner fails immediately after the write, because the backup already has the data.
*   **Cluster Management & Rebalancing:** Hazelcast monitors cluster health.
    *   **Member Leaves (Crash/Shutdown):** If a member leaves, Hazelcast promotes backups to primaries for the partitions owned by the lost member. It then creates new backups on other available members to restore the desired backup count. Data is migrated as needed.
    *   **Member Joins:** When a new member joins, Hazelcast rebalances the partition ownership across the cluster to distribute the load evenly.

## Listener Endpoint (`/listener`)

*   **Purpose:** The `/listener` endpoint calls `getCache().addEntryListener(...)` to register event listeners within the Hazelcast cluster for the "default" `IMap`.
*   **How it Works:**
    *   When an entry event (add, remove, update, evict) occurs on the **primary owner** for a partition, the Hazelcast eventing system is triggered *after* the operation (and its backups) are complete.
    *   It invokes the registered listener code (e.g., `EntryAddedListener`, `EntryRemovedListener`) on one or more members (Hazelcast manages listener execution distribution).
    *   In this project, the listeners simply log the event details.
*   **Use Cases:**
    *   **Real-time Monitoring/Debugging:** Observe cache activity across the cluster.
    *   **Cache Invalidation:** Trigger invalidation of related data in other systems or caches.
    *   **Data Synchronization with External Systems:** Push cache changes to databases, message queues, or WebSockets.
    *   **Reactive Workflows:** Build applications that react dynamically to state changes in the distributed cache.

## Failure Scenarios (Brief)

*   **Primary Fails During Write:** If the primary fails *before* acknowledging the backup, the write fails, and the client receives an error. If it fails *after* the backup acknowledges but *before* responding to the client, the write succeeded in the cluster, but the client might get an error (requiring retry logic or idempotency). The backup will be promoted.
*   **Network Partition (Split Brain):** If the cluster splits into subgroups that cannot communicate, Hazelcast has configurable "Split Brain Protection" and "Split Brain Merge Policies" to handle data consistency and cluster reunification when connectivity is restored. This requires careful configuration based on application needs.


Some other important Hazelcast concepts and features that are commonly used and could be valuable to explore:

1.  **Other Distributed Data Structures:**
    *   **`IQueue`:** A distributed, blocking queue. Useful for producer-consumer scenarios across the cluster.
    *   **`ITopic`:** A distributed publish-subscribe messaging system. Allows multiple members to subscribe to messages published on a topic.
    *   **`ISet` / `IList`:** Distributed implementations of Set and List interfaces.
    *   **`MultiMap`:** A specialized map where each key can be associated with multiple values.
    *   **`Ringbuffer`:** A fixed-size, sequence-preserving data structure where old items are overwritten when capacity is reached. Useful for event streams or keeping recent history.

2.  **Entry Processors:**
    *   Allow you to execute custom Java code directly on the primary owner node of a map entry **atomically**. This avoids network overhead for read-modify-write operations (e.g., incrementing a counter in the map) and ensures consistency without explicit locking in many cases.

3.  **MapStore / MapLoader (Persistence):**
    *   Integrates your `IMap` with a persistent backend store (like a database, file system, or external service).
    *   **MapLoader:** Loads data into the map from the backend store when an entry is requested but not found in memory.
    *   **MapStore:** Writes changes made to the map back to the backend store (either synchronously - write-through, or asynchronously - write-behind). This is crucial for caches that need to be backed by a system of record.

4.  **Near Cache:**
    *   A local, secondary cache kept on the client or member side that initiated the request. It stores frequently accessed entries locally to avoid repeated network hops to the primary owner, significantly improving read performance for hot keys. It comes with its own eviction and invalidation mechanisms.

5.  **CP Subsystem:**
    *   Provides distributed data structures and coordination primitives built on the Raft consensus algorithm, offering **linearizability** (stronger consistency than eventual consistency, often required for distributed coordination).
    *   Includes `CPSubsystem.getAtomicLong()`, `CPSubsystem.getLock()`, `CPSubsystem.getSemaphore()`, `CPSubsystem.getCountDownLatch()`, etc. Useful for scenarios requiring strict coordination like distributed counters, leader election, or distributed locking with fencing tokens.

6.  **Distributed Executor Service:**
    *   Allows you to submit tasks (implementing `Runnable` or `Callable`) for execution on specific members, all members, or members owning specific keys within the cluster.

7.  **Aggregations:**
    *   Perform aggregate calculations (like sum, average, count, max, min) directly on the data stored in distributed maps across the cluster, often much faster than retrieving all data and aggregating locally.

8.  **Client-Server Topology:**
    *   Instead of embedding Hazelcast members within each application pod (as done currently), you can run dedicated Hazelcast server nodes forming the cluster. Your application pods then connect as lightweight **clients** to this external cluster. This separates caching concerns from application logic and allows independent scaling of the cache cluster.

These features represent different facets of Hazelcast's capabilities, from alternative data patterns and persistence to performance optimization and stronger consistency models. 