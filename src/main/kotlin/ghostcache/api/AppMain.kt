package ghostcache.api

import io.grpc.netty.NettyServerBuilder
import grpc.ghostcache.CacheServiceImpl
import grpc.ghostcache.auth.JwtAuthInterceptor
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec

fun main() {
    val grpcPort = System.getenv("GRPC_PORT")?.toInt() ?: 50051
    val redisUri = System.getenv("REDIS_URL") ?: "redis://localhost:6379"
    val jwtSecret = System.getenv("JWT_SECRET")
        ?: error("JWT_SECRET environment variable not set")

    // Redis setup
    val client = RedisClient.create(redisUri)
    val connection = client.connect(ByteArrayCodec())
    val commands = connection.sync()

    // gRPC Server
    val grpcServer = NettyServerBuilder
        .forPort(grpcPort)
        .intercept(JwtAuthInterceptor(jwtSecret))
        .addService(CacheServiceImpl(commands))
        .build()
        .start()

    println("✅ gRPC server listening on port $grpcPort")

    // Graceful shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        grpcServer.shutdown()
        connection.close()
        client.shutdown()
    })

    grpcServer.awaitTermination()
}
