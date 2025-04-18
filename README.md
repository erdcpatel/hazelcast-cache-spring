# Hazelcast Cache Spring Application

This project demonstrates the use of Hazelcast 5.3+ features in a Spring Boot 3.2+ application. It includes auto-configured embedded caching, distributed map storage, entry eviction policies, REST endpoints, health checks (Actuator), Lombok, and Swagger documentation.

## Prerequisites

- Java 17+ (Verify with `java -version`)
- Maven 3.9+ (Verify with `mvn -v`)
- Docker (Verify with `docker --version`)
- Kubernetes (Minikube or any other cluster, verify with `kubectl version`)
- `curl` or a similar tool for testing APIs

## Local Development

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd hazelcast-cache-spring
    ```

2.  **Generate Maven Wrapper (if not present):**
    *(The wrapper files `mvnw` and `mvnw.cmd` should already be included)*
    ```bash
    # If needed: mvn -N io.takari:maven:wrapper
    ```

3.  **Build the project:**
    ```bash
    ./mvnw clean verify
    ```
    *(This compiles, runs tests, and packages the application)*

4.  **Run the application:**
    ```bash
    ./mvnw spring-boot:run
    ```
    *(The application will start on port 8080 by default for local development)*

5.  **Access the Swagger UI:**
    - URL: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

6.  **Access Health Checks:**
    - Liveness: [http://localhost:8080/actuator/health/liveness](http://localhost:8080/actuator/health/liveness)
    - Readiness: [http://localhost:8080/actuator/health/readiness](http://localhost:8080/actuator/health/readiness)
    - General Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

7.  **Debug in VS Code:**
    - Use the provided `.vscode/launch.json` configuration to debug the Spring Boot application directly from VS Code.

## Sample API Requests (Local)

```bash
# Add an entry
curl -X PUT -H "Content-Type: application/json" -d '"myValue"' http://localhost:8080/cache/myKey

# Get an entry
curl http://localhost:8080/cache/myKey

# Get cache stats
curl http://localhost:8080/cache/stats

# Query entries with prefix "my"
curl "http://localhost:8080/cache/query?keyPrefix=my"

# Lock a key
curl -X POST http://localhost:8080/cache/lock/myKey

# Unlock a key
curl -X POST http://localhost:8080/cache/unlock/myKey

# Delete an entry
curl -X DELETE http://localhost:8080/cache/myKey

# --- Ringbuffer Examples ---

# Add an item to the ringbuffer (returns sequence number)
curl -X POST -H "Content-Type: text/plain" -d 'event1' http://localhost:8080/ringbuffer/add
curl -X POST -H "Content-Type: text/plain" -d 'event2' http://localhost:8080/ringbuffer/add

# Read items starting from sequence 0
curl "http://localhost:8080/ringbuffer/read?startSequence=0"

# Get ringbuffer info
curl http://localhost:8080/ringbuffer/info
```

## Docker Usage

1.  **Build the Docker image:**
    *(Ensure `.dockerignore` is present to optimize the build)*
    ```bash
    docker build -t hazelcast-cache-spring:latest .
    ```

2.  **Run the Docker container:**
    *(Maps container port 8080 to host port 8080)*
    ```bash
    docker run -p 8080:8080 hazelcast-cache-spring:latest
    ```

3.  **Access the application (via Docker):**
    - Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
    - Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
    - *Use port 8080 for API requests when running via Docker.*

## Kubernetes Deployment

1.  **Ensure your Docker image is accessible to Kubernetes:**
    - If using Minikube, you can build the image within Minikube's Docker daemon:
      ```bash
      eval $(minikube -p minikube docker-env)
      docker build -t hazelcast-cache-spring:latest .
      eval $(minikube -p minikube docker-env -u) # Unset env
      ```
    - Alternatively, push the image to a registry (like Docker Hub, GCR, etc.) and update the `image:` field in `deployment.yaml`. Ensure `imagePullPolicy: Always` or `IfNotPresent` is set appropriately in the deployment.

2.  **Apply the Kubernetes manifests:**
    *(This creates the ConfigMap, Deployment, and Service)*
    ```bash
    kubectl apply -f deployment.yaml
    ```

3.  **Verify the deployment:**
    ```bash
    kubectl get pods -l app=hazelcast-cache-spring # Wait until STATUS is Running (may take a minute)
    kubectl get svc hazelcast-cache-spring
    kubectl get configmap hazelcast-config
    ```
    *(Expect 3 pods due to `replicas: 3`)*

4.  **Check Logs (Optional):**
    ```bash
    kubectl logs -l app=hazelcast-cache-spring -f --tail=100
    ```
    *(Look for Hazelcast cluster membership logs)*

5.  **Access the application within Kubernetes:**
    - Use `kubectl port-forward` to access the service locally:
      ```bash
      kubectl port-forward svc/hazelcast-cache-spring 8080:8080
      ```
    - Now access via localhost:8080:
      - Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
      - Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
      - *Use port 8080 for API requests when port-forwarding.*

## Troubleshooting

-   **Port Conflicts (Local):** Ensure port 8080 (or the configured `server.port`) is free. Use `lsof -i :8080` or similar to check.
-   **Kubernetes DNS Issues:**
    -   Verify the `service-dns` property in `application.properties` inside the `ConfigMap` matches the Kubernetes service name (`hazelcast-cache-spring`) and namespace (`default`). The format is typically `<service-name>.<namespace>.svc.cluster.local`.
    -   Check Hazelcast logs (`kubectl logs ...`) for discovery errors.
    -   Ensure CoreDNS (or your cluster's DNS provider) is running correctly (`kubectl get pods -n kube-system`).
-   **Docker Build Errors:** Check the `Dockerfile` syntax. Ensure the build context (`.`) is correct. Check `.dockerignore`.
-   **ImagePullBackOff (Kubernetes):** The cluster cannot pull the `hazelcast-cache-spring:latest` image. Ensure the image exists in the cluster's registry or the specified remote registry and that `imagePullPolicy` is correct.
-   **CrashLoopBackOff (Kubernetes):** Pods are failing to start. Check pod logs (`kubectl logs <pod-name>`) for application errors (e.g., configuration issues, Hazelcast startup problems). Increase `initialDelaySeconds` for probes if the application takes longer to start.
-   **Lombok Issues:** Ensure your IDE has Lombok plugin installed and annotation processing is enabled.

## Features Demonstrated

-   Hazelcast distributed map storage (`IMap`) via `CacheMapController`
-   Hazelcast distributed ringbuffer (`Ringbuffer`) via `RingbufferController`
-   Entry eviction policies (LRU, configured size)
-   Time-To-Live (TTL) for entries
-   Querying cache entries using `Predicates`
-   Distributed locking (`IMap.lock()`, `IMap.unlock()`)
-   Cache event listeners (`EntryAddedListener`, `EntryRemovedListener`)
-   Kubernetes DNS discovery configuration
-   Spring Boot Actuator Health Checks (Liveness/Readiness)
-   Swagger API documentation (`springdoc-openapi`)
-   Lombok for reduced boilerplate code
-   Multi-stage Docker build
-   Maven Wrapper

## Learning Objectives

This project serves as a practical example for learning Hazelcast caching and clustering features within a modern Spring Boot application. Explore the code (`CacheMapController`, `HazelcastConfig`), review the configuration (`application.properties`, `deployment.yaml`), interact with the API via Swagger UI or `curl`, and observe the logs to understand the behavior in both local and Kubernetes environments.