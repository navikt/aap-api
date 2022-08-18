package routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Routing.actuators(prometheus: PrometheusMeterRegistry) {
    route("/actuator") {
        get("/metrics") {
            val metrics = prometheus.scrape()
            call.respondText(metrics)
        }

        get("/live") {
            call.respond(HttpStatusCode.OK, "api")
        }

        get("/ready") {
            call.respond(HttpStatusCode.OK, "api")
        }
    }
}
