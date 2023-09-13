package api.routes

import io.github.smiley4.ktorswaggerui.dsl.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.dto.kafka.IverksettVedtakKafkaDto
import no.nav.aap.kafka.streams.v2.StateStore
import java.time.LocalDate
import java.util.*

fun Routing.vedtak(vedtakStore: StateStore<IverksettVedtakKafkaDto>) {

    val dummy = IverksettVedtakKafkaDto(
        vedtaksid = UUID.randomUUID(),
        innvilget = false,
        grunnlagsfaktor = 0.0,
        vedtaksdato = LocalDate.now(),
        virkningsdato = LocalDate.now(),
        fødselsdato = LocalDate.now()
    )

    authenticate {
        get("/vedtak/{personident}", {
            securitySchemeNames = setOf("Maskinporten")
            response {
                HttpStatusCode.OK to {
                    description = "Henter et vedtak"
                    body<IverksettVedtakKafkaDto> {}
                }
            }
        }) {
            val personident: String = call.parameters.getOrFail("personident")

            when (val vedtak = vedtakStore[personident]) {
                null -> call.respond(HttpStatusCode.OK, dummy)
                else -> call.respond(HttpStatusCode.OK, vedtak)
            }
        }
    }
}
