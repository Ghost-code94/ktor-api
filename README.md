The Cache-Pipeline API

The Cache-Pipeline API is a lightweight, edge-optimized server built with Kotlin and gRPC, designed for high-performance caching tasks in containerized and cloud environments.

Originally scaffolded with Ktor, this project now exposes a pure gRPC interface and serves as a modular, extensible backend service for any system needing fast, binary-efficient access to cache-like storage.

Features

gRPC Server Only — Runs a full gRPC server on a configurable port (default: 50051).

Redis-Backed Caching — Uses Redis for fast key-value storage with support for TTL (time-to-live).

Versioned Cache Operations — Supports storing multiple versions of a key, querying history, and rolling back to any prior version.

Protobuf-Defined Interface — RPC endpoints are clearly defined in cache.proto, supporting cross-language clients.

Portable via Docker — Easily deployable in cloud or local environments using Docker.

Available RPCs

The gRPC service CacheService provides:

Method

Description

Put

Store a binary value with a TTL and create a new version entry.

Get

Retrieve the latest value by key.

GetVersioned

Retrieve a specific or latest version of a key.

Invalidate

Delete a key (latest value) from the cache.

ListKeys

Stream all current keys from Redis.

History

Stream version history (IDs & timestamps) for a given key.

Rollback

Roll back a key to a specified version (restoring its value).

All messages and requests are defined in src/main/proto/ghostcache/cache.proto.

Getting Started

Run Redis (locally for dev)

docker run --rm -d --name redis-test -p 6379:6379 redis:latest

Run the gRPC server

docker run -d \
  -p 50051:50051 \
  -e REDIS_URL=redis://host.docker.internal:6379 \
  yourdockerrepository/ktor-server-io:latest

Replace host.docker.internal with your Redis host or service name if on the same Docker network.

Calling the API

You can test the service using grpcurl.

List available RPCs

grpcurl -plaintext \
  -import-path src/main/proto \
  -proto ghostcache/cache.proto \
  localhost:50051 \
  list ghostcache.CacheService

Put a value (no auth)

grpcurl -plaintext \
  -import-path src/main/proto \
  -proto ghostcache/cache.proto \
  -d '{"key":"foo","value":"YmFy","ttlSec":60}' \
  localhost:50051 \
  ghostcache.CacheService/Put

Get the latest value (no auth)

grpcurl -plaintext \
  -import-path src/main/proto \
  -proto ghostcache/cache.proto \
  -d '{"key":"foo"}' \
  localhost:50051 \
  ghostcache.CacheService/Get

List version history (no auth)

grpcurl -plaintext \
  -import-path src/main/proto \
  -proto ghostcache/cache.proto \
  -d '{"key":"foo"}' \
  localhost:50051 \
  ghostcache.CacheService/History

Get a specific (or latest) version (no auth)

grpcurl -plaintext \
  -import-path src/main/proto \
  -proto ghostcache/cache.proto \
  -d '{"key":"foo","version":"<VERSION_ID>"}' \
  localhost:50051 \
  ghostcache.CacheService/GetVersioned

Passing an empty string for version returns the latest.

Rollback to an earlier version (no auth)

grpcurl -plaintext \
  -import-path src/main/proto \
  -proto ghostcache/cache.proto \
  -d '{"key":"foo","version":"<VERSION_ID>"}' \
  localhost:50051 \
  ghostcache.CacheService/Rollback

Authorization

This service can be secured with JWT-based authentication. To require and validate tokens, a JwtAuthInterceptor is applied to the gRPC server. All RPCs then expect an authorization: Bearer <token> metadata header.

Generating a Token

create an HS256 token signed with your JWT_SECRET. Example header:

export JWT_SECRET="<your-base64-secret>"

Calling with Authorization

grpcurl -insecure \
  -authority localhost:50051 \
  -import-path src/main/proto \
  -proto ghostcache/cache.proto \
  -H "authorization: Bearer <your-jwt>" \
  -d '{"key":"foo","value":"YmFy","ttlSec":60}' \
  localhost:50051 \
  ghostcache.CacheService/Put

Calls without a valid token will return UNAUTHENTICATED.

Ensure you hit the correct port and use -insecure or trust your TLS certificate when running in production.