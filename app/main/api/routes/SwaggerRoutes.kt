package api.routes

import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Routing.swaggerRoutes() {
    swaggerUI(path = "/", swaggerFile = "openapi.yml")
}
