package vedtak

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

fun Routing.vedtak(config: Config, httpClient: HttpClient) {
    val jackson = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    get("/vedtak/{personident}") {
        val vedtaksdato: String? = call.parameters["vedtaksdato"]
        val personident: String = call.parameters.getOrFail("personident")

        val vedtakDao = httpClient.get("${config.sinkHost}/vedtak/$personident/latest").body<Dao>()
        val vedtakDto = jackson.readValue<VedtakKafkaDto>(vedtakDao.record)

        call.respond(HttpStatusCode.OK, vedtakDto)
    }
}
