package ghostcache.api

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.*

fun main() {
    // Use dynamic port (for example, when run in a containerized environment)
    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port) {
        install(DefaultHeaders)
        install(CallLogging)
        routing {
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Plain)
            }
            // Additional endpoints can be added here.
        }
    }.start(wait = true)
}
