package grpc.ghostcache

import ghostcache.*
import io.grpc.stub.StreamObserver
import io.lettuce.core.api.sync.RedisCommands
import com.google.protobuf.ByteString

class CacheServiceImpl(
    private val redis: RedisCommands<String, ByteArray>
) : CacheServiceGrpcKt.CacheServiceCoroutineImplBase() {

    override suspend fun get(request: GetRequest): GetReply {
        val data = redis.get(request.key)
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
        redis.setex(request.key, request.ttlSec, request.value.toByteArray())
        return PutReply.newBuilder().setOk(true).build()
    }

    override suspend fun invalidate(request: InvalidateRequest): InvalidateReply {
        val result = redis.del(request.key) > 0
        return InvalidateReply.newBuilder().setOk(result).build()
    }

    override fun listKeys(request: Empty, responseObserver: StreamObserver<KeyEntry>) {
        val cursor = redis.scan()
        cursor.forEach { key ->
            responseObserver.onNext(KeyEntry.newBuilder().setKey(key).build())
        }
        responseObserver.onCompleted()
    }
}
