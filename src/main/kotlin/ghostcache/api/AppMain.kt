package ghostcache.api

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.response.respondText
import io.ktor.http.ContentType
import io.grpc.ServerBuilder
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec
import grpc.ghostcache.CacheServiceImpl
import kotlinx.coroutines.runBlocking

fun main() {
    // ----- Configuration -----
    val httpPort = System.getenv("PORT")?.toInt() ?: 8080
    val grpcPort = System.getenv("GRPC_PORT")?.toInt() ?: 50051
    val redisUri = System.getenv("REDIS_URL") ?: "redis://localhost:6379"

    // ----- Redis Setup -----
    val client = RedisClient.create(redisUri)
    // Use ByteArrayCodec for storing raw bytes
    val connection = client.connect(ByteArrayCodec(), ByteArrayCodec())
    val commands = connection.sync()

    // ----- gRPC Server -----
    val grpcServer = ServerBuilder
        .forPort(grpcPort)
        .addService(CacheServiceImpl(commands))
        .build()
        .start()
    println("gRPC server listening on port $grpcPort")

    // ----- Ktor HTTP Server -----
    val ktorServer = embeddedServer(Netty, port = httpPort) {
        install(DefaultHeaders)
        install(CallLogging)
        routing {
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Plain)
            }
            // Additional HTTP endpoints can be added here.
        }
    }.start()
    println("HTTP server listening on port $httpPort")

    // ----- Shutdown Hook -----
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down HTTP and gRPC servers...")
        ktorServer.stop(1000, 2000)
        grpcServer.shutdown()
        connection.close()
        client.shutdown()
    })

    // Block main thread until gRPC server is terminated
    grpcServer.awaitTermination()
}
