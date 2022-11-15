package routes

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.swaggerRoutes() {
    static("static") {
        resources("main")
    }
    get("/") {
        call.respondRedirect("/webjars/swagger-ui/index.html?url=/static/openapi.yml")
    }
}