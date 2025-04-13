# Prompt Collection

## Spring Boot + Hazelcast Learning Application

### Description
Create a **Spring Boot 3.2+** Java application with Maven that demonstrates **Hazelcast 5.3+** features, including:

1. **Auto-configured embedded caching**
2. **Distributed map storage**
3. **Entry eviction policies**
4. **REST endpoints** to interact with the cache
5. **Health checks**
6. Add **Swagger documentation** with detailed API descriptions (professional and enterprise-ready for a session)
7. Cover as many Hazelcast features as possible for **learning purposes** (targeted at beginners)
8. Include **detailed logs** to help understand the API behavior during interactions.

---

### Requirements

#### Project Setup
- Start from scratch (no existing project).
- Use **Maven wrapper**.
- Include **Lombok annotations**.
- Add **Swagger documentation**.

#### REST Endpoints
Create a `@RestController` with the following cache operations:
- **PUT /cache/{key}** - Add an entry to the cache.
- **GET /cache/{key}** - Retrieve an entry from the cache.
- **DELETE /cache/{key}** - Remove an entry from the cache.
- **GET /cache/stats** - Show cache metrics.
- Add any additional endpoints required to explore Hazelcast features.

#### Configuration
- Configure Hazelcast for:
  - **Local development**
  - **Kubernetes deployment** (include DNS discovery).
- Add a **Dockerfile** with a multi-stage build (Java 17 base).
- Add a **Kubernetes deployment.yaml** with:
  - Liveness/readiness probes.
  - Service exposure (ClusterIP).
  - 3 replicas.
  - ConfigMap for application properties.

---

### Technical Setup
1. Verify **Java 17+** is installed (use **SDKMAN!** if missing).
2. Ensure **Maven 3.9+** is available.
3. Generate the **mvnw wrapper** if not present.
4. Create a `.vscode/launch.json` for Spring Boot debugging.
5. Include a `.dockerignore` file with proper patterns.
6. Add a **kubectl deployment checklist**.

---

### Documentation
Create a `README.md` with:
- **Prerequisites**
- **Local development instructions**
- **Docker build/run commands**
- **Kubernetes deployment steps**
- **Sample API requests**
- **Troubleshooting common issues** (e.g., port conflicts, discovery problems).

---

### Validation Plan
1. Perform a **local Maven build** (`mvn clean verify`).
2. Run the application via the **Spring Boot Maven plugin**.
3. Test API endpoints using **curl**.
4. Build a Docker image and run it locally.
5. Deploy to a Kubernetes cluster (e.g., **minikube**) and verify cluster formation.

---

### Model used with VS Code agent mode:
* GPT-4o
* gemini-2.5-pro-preview-03-25