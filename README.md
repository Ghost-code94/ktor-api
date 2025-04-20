# GhostCache gRPC Service

A robust, Redis-backed gRPC caching service with built-in versioning and JWT-based authentication.

## 📦 Overview

GhostCache exposes a simple, high-performance caching API over gRPC. It supports:

- **Basic Caching Operations**: `get`, `put` (with TTL), `invalidate`, and `listKeys`.
- **Versioning**: Store and manage historical versions of cached values. Retrieve specific versions, view history, and rollback to previous states.
- **Security**: Protect access using JSON Web Tokens (JWT), validated by an interceptor on every gRPC call.

## 🏛️ Architecture

- **`CacheServiceImpl`** (`grpc.ghostcache.CacheServiceImpl`)
  - Implements the core gRPC service logic against Redis:
    - Stores live values with expiration and tracks versions in a sorted set and hash.
    - Retrieves values (latest or specific version), lists all keys, fetches version history, and rolls back to prior versions.

- **`JwtAuthInterceptor`** (`grpc.ghostcache.auth.JwtAuthInterceptor`)
  - A server interceptor that:
    - Extracts and validates the `Bearer` token from call metadata.
    - Parses JWT claims using a signing secret.
    - Embeds claims into the gRPC context for downstream authorization logic.

- **Entrypoint** (`ghostcache.api.Main`)
  - Reads configuration via environment variables (`GRPC_PORT`, `REDIS_URL`, `JWT_SECRET`).
  - Initializes a Lettuce Redis client for byte-array operations.
  - Builds and starts a Netty-based gRPC server with the JWT interceptor and caching service.
  - Registers a shutdown hook for graceful termination of the server and Redis connection.

## 🚀 Getting Started

1. **Clone the repository**:

   ```bash
   git clone https://github.com/your-org/ghostcache.git
   cd ghostcache
   ```

2. **Configure environment**:

   ```bash
   export GRPC_PORT=50051         # Port for gRPC server (default: 50051)
   export REDIS_URL=redis://localhost:6379  # Redis URI
   export JWT_SECRET=your-secret-key        # Secret for JWT validation
   ```

3. **Build & Run**:

   ```bash
   ./gradlew run
   ```

4. **Use the Service**:

   Connect any gRPC client and invoke methods defined in the `CacheService` protobuf. Ensure you supply a valid JWT in the `authorization` metadata header.

## 🔧 Configuration

| Variable    | Description                                      | Default           |
|-------------|--------------------------------------------------|-------------------|
| `GRPC_PORT` | Port on which the gRPC server listens            | `50051`           |
| `REDIS_URL` | Redis connection URI                             | `redis://localhost:6379` |
| `JWT_SECRET`| Secret key for signing and verifying JWT tokens  | **Required**      |

## 📄 License

This project is released under the MIT License. Feel free to use and modify it as needed.