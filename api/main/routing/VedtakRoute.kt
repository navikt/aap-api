package routing

import Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*

fun Routing.vedtak(config: Config, httpClient: HttpClient) {
    get("/vedtak/{personident}") {
        val personident = call.parameters.getOrFail("personident")

        val søker = httpClient.get("${config.sinkHost}/soker/$personident/latest") {
//            accept(ContentType.Application.Json)
//            contentType(ContentType.Application.Json)
        }.body<SøkerDao>()

        call.respond(HttpStatusCode.OK, søker)
    }
}

data class SøkerDao(
    val personident: String,
    val record: String,
    val dtoVersion: Int?,
    val partition: Int,
    val offset: Long,
    val topic: String,
    val timestamp: Long,
    val systemTimeMs: Long,
    val streamTimeMs: Long,
)
