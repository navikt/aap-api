package søker

import Config
import Dao
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*

fun Routing.søker(config: Config, httpClient: HttpClient) {
    get("/soker/{personident}") {
        val vedtaksdato: String? = call.parameters["vedtaksdato"]
        val personident: String = call.parameters.getOrFail("personident")

        val søker = httpClient
            .get("${config.sinkHost}/soker/$personident/latest")
            .body<Dao>()

        call.respond(HttpStatusCode.OK, søker)
    }
}
