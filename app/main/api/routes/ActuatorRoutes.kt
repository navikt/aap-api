package api.routes

import io.github.smiley4.ktorswaggerui.dsl.route
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.kafka.streams.v2.Streams

fun Routing.actuatorRoutes(
    prometheus: PrometheusMeterRegistry,
    kafka: Streams,
) {
    route("/actuator", {
        hidden = true
    }) {
        get("/metrics") {
            call.respondText(prometheus.scrape())
        }

        get("/live") {
            when (kafka.live()) {
                true -> call.respond(HttpStatusCode.OK, "api")
                false -> call.respond(HttpStatusCode.InternalServerError, "api")
            }
        }
        get("/ready") {
            when (kafka.ready()) {
                true -> call.respond(HttpStatusCode.OK, "api")
                false -> call.respond(HttpStatusCode.InternalServerError, "api")
            }
        }
    }
}
