The Cache-Pipeline API is a lightweight, edge-optimized server built with Kotlin and gRPC, designed for high-performance caching tasks in containerized and cloud environments.

Originally scaffolded with Ktor, the project now exposes a pure gRPC interface and serves as a modular, extensible backend service for any system needing fast, binary-efficient access to cache-like storage.

✅ Features

🔌 gRPC Server Only — Runs a full gRPC server on a configurable port (default: 50051).
🚀 Redis-Backed Caching — Uses Redis for fast key-value storage with support for TTL (time-to-live).
💡 Protobuf-Defined Interface — RPC endpoints are clearly defined in cache.proto, supporting cross-language clients.
🧪 Portable via Docker — Easily deployable in cloud or local environments using Docker.
🛠️ Available RPCs

The gRPC service CacheService provides:


Method	Description
Put	Store a binary value with a TTL
Get	Retrieve a value by key
Invalidate	Delete a key from the cache
ListKeys	Stream all current keys from Redis
All messages and requests are defined in src/main/proto/ghostcache/cache.proto.

🚀 Getting Started

Run Redis (locally for dev)
docker run --rm -d --name redis-test -p 6379:6379 redis:latest
Run the gRPC server
docker run -d \
  -p 50051:50051 \
  -e REDIS_URL=redis://host.docker.internal:6379 \
  ghostcode94/ktor-server-io:latest
🐳 Replace host.docker.internal with redis-test if both are on the same Docker network.
🔧 Calling the API

You can test the service using grpcurl:

# List available RPCs
grpcurl -plaintext -import-path src/main/proto \
  -proto ghostcache/cache.proto \
  localhost:50051 list ghostcache.CacheService

# Put a value
grpcurl -plaintext -import-path src/main/proto \
  -proto ghostcache/cache.proto \
  -d '{"key":"foo","value":"YmFy","ttlSec":60}' \
  localhost:50051 ghostcache.CacheService/Put
