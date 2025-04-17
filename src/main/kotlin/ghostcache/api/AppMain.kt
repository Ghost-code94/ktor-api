package ghostcache.api

import io.grpc.netty.NettyServerBuilder
import grpc.ghostcache.CacheServiceImpl
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec

fun main() {
    val grpcPort = System.getenv("GRPC_PORT")?.toInt() ?: 50051
    val redisUri = System.getenv("REDIS_URL") ?: "redis://localhost:6379"

    // Redis Setup
    val client = RedisClient.create(redisUri)
    val connection = client.connect(ByteArrayCodec())
    val commands = connection.sync()

    // gRPC Server only
    val grpcServer = NettyServerBuilder
        .forPort(grpcPort)
        .addService(CacheServiceImpl(commands))
        .build()
        .start()
    println("gRPC server listening on port $grpcPort")

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down gRPC server and Redis connection...")
        grpcServer.shutdown()
        connection.close()
        client.shutdown()
    })

    grpcServer.awaitTermination()
}
