package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.kafka.streams.KStreams

fun Routing.actuatorRoutes(
    prometheus: PrometheusMeterRegistry,
    kafka: KStreams,
) {
    route("/actuator") {
        get("/metrics") {
            call.respondText(prometheus.scrape())
        }

        get("/live") {
            when (kafka.isLive()) {
                true -> call.respond(HttpStatusCode.OK, "api")
                false -> call.respond(HttpStatusCode.InternalServerError, "api")
            }
        }
        get("/ready") {
            when (kafka.isReady()) {
                true -> call.respond(HttpStatusCode.OK, "api")
                false -> call.respond(HttpStatusCode.InternalServerError, "api")
            }
        }
    }
}
