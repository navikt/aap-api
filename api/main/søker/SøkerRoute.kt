package søker

import Config
import Dao
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*

fun Routing.søker(config: Config, httpClient: HttpClient) {
    val jackson = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    get("/soker/{personident}") {
        val vedtaksdato: String? = call.parameters["vedtaksdato"]
        val personident: String = call.parameters.getOrFail("personident")

        val søkerDao = httpClient.get("${config.sinkHost}/soker/$personident/latest").body<Dao>()
        val søkerDto = jackson.readValue<SøkereKafkaDto>(søkerDao.record)

        call.respond(HttpStatusCode.OK, søkerDto)
    }
}
