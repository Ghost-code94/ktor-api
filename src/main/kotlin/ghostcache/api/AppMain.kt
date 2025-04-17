package ghostcache.api

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.install
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.routing.routing
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.http.ContentType

// <-- use the un‑shaded NettyServerBuilder
import io.grpc.netty.NettyServerBuilder

import grpc.ghostcache.CacheServiceImpl

import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec

fun main() {
    val httpPort = System.getenv("PORT")?.toInt() ?: 8080
    val grpcPort = System.getenv("GRPC_PORT")?.toInt() ?: 50051
    val redisUri = System.getenv("REDIS_URL") ?: "redis://localhost:6379"

    // Redis Setup
    val client = RedisClient.create(redisUri)
    val connection = client.connect(ByteArrayCodec())
    val commands = connection.sync()

    // gRPC Server (un‑shaded Netty transport)
    val grpcServer = NettyServerBuilder
        .forPort(grpcPort)
        .addService(CacheServiceImpl(commands))
        .build()
        .start()
    println("gRPC server listening on port $grpcPort")

    // Ktor HTTP Server
    val ktorServer = embeddedServer(Netty, port = httpPort) {
        install(DefaultHeaders)
        install(CallLogging)
        routing {
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Plain)
            }
        }
    }.start()
    println("HTTP server listening on port $httpPort")

    Runtime.getRuntime().addShutdownHook(Thread {
        ktorServer.stop(1_000, 2_000)
        grpcServer.shutdown()
        connection.close()
        client.shutdown()
    })

    grpcServer.awaitTermination()
}
