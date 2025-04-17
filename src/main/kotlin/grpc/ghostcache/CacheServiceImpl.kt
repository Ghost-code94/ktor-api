package grpc.ghostcache

import ghostcache.*                             // your proto types
import io.grpc.stub.StreamObserver
import io.lettuce.core.api.sync.RedisCommands
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CacheServiceImpl(
    private val redis: RedisCommands<ByteArray, ByteArray>
) : CacheServiceGrpcKt.CacheServiceCoroutineImplBase() {

    override suspend fun get(request: GetRequest): GetReply {
        val data = redis.get(request.key.toByteArray())
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

    override suspend fun put(request: PutRequest): PutReply {
        redis.setex(request.key.toByteArray(), request.ttlSec.toLong(), request.value.toByteArray())
        return PutReply.newBuilder().setOk(true).build()
    }

    override suspend fun invalidate(request: InvalidateRequest): InvalidateReply {
        val result = redis.del(request.key.toByteArray()) > 0
        return InvalidateReply.newBuilder().setOk(result).build()
    }

    // Server‐streaming → return a Flow<KeyEntry>
    override fun listKeys(request: Empty): Flow<KeyEntry> = flow {
        val cursor = redis.scan()
        for (keyBytes in cursor) {
            emit(
                KeyEntry.newBuilder()
                    .setKey(String(keyBytes))
                    .build()
            )
        }
    }
}
