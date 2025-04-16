package ghostcache.api

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

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
