// src/main/kotlin/grpc/ghostcache/CacheServiceImpl.kt
package grpc.ghostcache

import ghostcache.*
import io.lettuce.core.ScoredValue
import io.lettuce.core.api.sync.RedisCommands
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CacheServiceImpl(
    private val redis: RedisCommands<ByteArray, ByteArray>,
    private val maxVersions: Long = 100
) : CacheServiceGrpcKt.CacheServiceCoroutineImplBase() {

    // Redis key helpers
    private fun rawKey(key: String) = key.toByteArray()
    private fun versionSetKey(key: String) = "$key:versions".toByteArray()
    private fun dataHashKey(key: String) = "$key:data".toByteArray()

    //------ basic ops unchanged ------

    override suspend fun get(request: GetRequest): GetReply {
        val data = redis.get(rawKey(request.key))
        return if (data != null) {
            GetReply.newBuilder()
                .setFound(true)
                .setValue(ByteString.copyFrom(data))
                .build()
        } else {
            GetReply.newBuilder().setFound(false).build()
        }
    }

    override suspend fun put(request: PutRequest): PutReply {
        val key       = request.key
        val rawKey    = rawKey(key)
        val versions  = versionSetKey(key)
        val dataHash  = dataHashKey(key)

        // 1) store the live value with TTL
        redis.setex(rawKey, request.ttlSec.toLong(), request.value.toByteArray())

        // 2) record a new version
        val versionId = System.currentTimeMillis().toString()
        redis.zadd(versions, versionId.toDouble(), versionId.toByteArray())
        redis.hset(dataHash, versionId.toByteArray(), request.value.toByteArray())

        // 3) prune old versions if we’ve exceeded maxVersions
        val count = redis.zcard(versions)
        if (count > maxVersions) {
            val excess = (count - maxVersions).toInt()

            // 3a) fetch the oldest version‐IDs
            val oldIds: List<ByteArray> = redis.zrange(versions, 0, excess - 1)

            // 3b) remove them from the sorted set
            redis.zremrangeByRank(versions, 0, excess - 1)

            // 3c) delete their payloads from the hash
            oldIds.forEach { id -> 
                redis.hdel(dataHash, id)
            }
        }

        return PutReply.newBuilder()
            .setOk(true)
            .build()
    }

    override suspend fun invalidate(request: InvalidateRequest): InvalidateReply {
        val deleted = redis.del(rawKey(request.key)) > 0
        return InvalidateReply.newBuilder().setOk(deleted).build()
    }

    override fun listKeys(request: Empty): Flow<KeyEntry> = flow {
        val scan = redis.scan()  // lettuce KeyScanCursor<ByteArray>
        for (k in scan.keys) {
            emit(KeyEntry.newBuilder().setKey(String(k)).build())
        }
    }

    //------ versioning RPCs ------

    override suspend fun getVersioned(request: VersionedGetRequest): GetReply {
        val key = request.key
        // determine which version to fetch
        val versionId = if (request.version.isBlank()) {
            // pick most recent — zrevrange returns List<ByteArray>
            val latest: List<ByteArray> = redis.zrevrange(versionSetKey(key), 0, 0)
            if (latest.isEmpty()) {
                return GetReply.newBuilder()
                    .setFound(false)
                    .build()
            }
            // convert the first entry back to string
            String(latest[0])
        } else {
            request.version
        }

        // fetch from hash
        val data = redis.hget(dataHashKey(key), versionId.toByteArray())
        return if (data != null) {
            GetReply.newBuilder()
                .setFound(true)
                .setValue(ByteString.copyFrom(data))
                .build()
        } else {
            GetReply.newBuilder()
                .setFound(false)
                .build()
        }
    }

    override suspend fun history(request: GetRequest): HistoryReply {
        val key = request.key
        val entries: List<ScoredValue<ByteArray>> =
            redis.zrangeWithScores(versionSetKey(key), 0, -1)
                .toList()

        val builder = HistoryReply.newBuilder()
        for (entry in entries) {
            val version = String(entry.value)
            val ts = entry.score.toLong()
            builder.addVersions(
                HistoryEntry.newBuilder()
                    .setVersion(version)
                    .setTimestamp(ts)
                    .build()
            )
        }
        return builder.build()
    }

    override suspend fun rollback(request: VersionedGetRequest): PutReply {
        val key = request.key
        val version = request.version
        // fetch the versioned blob
        val blob = redis.hget(dataHashKey(key), version.toByteArray())
            ?: return PutReply.newBuilder().setOk(false).build()

        // preserve existing TTL if any
        val ttl = redis.ttl(rawKey(key))
        if (ttl > 0) {
            redis.setex(rawKey(key), ttl, blob)
        } else {
            redis.set(rawKey(key), blob)
        }
        return PutReply.newBuilder().setOk(true).build()
    }
}
