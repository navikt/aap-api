package routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

fun Routing.meldepliktshendelser() {
    get("/vedtak") {
        call.respond(HttpStatusCode.OK)
    }
}
