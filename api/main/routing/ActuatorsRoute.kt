package routing

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Routing.actuators(prometheus: PrometheusMeterRegistry) {
    route("/actuator") {
        get("/metrics") { call.respondText(prometheus.scrape()) }
        get("/live") { call.respondText("api") }
        get("/ready") { call.respondText("api") }
    }
}
