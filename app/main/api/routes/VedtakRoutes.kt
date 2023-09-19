package api.routes

import api.arena.ArenaoppslagRequest
import api.arena.ArenaoppslagRestClient
import io.github.smiley4.ktorswaggerui.dsl.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.dto.kafka.IverksettVedtakKafkaDto
import no.nav.aap.kafka.streams.v2.StateStore
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.LocalDate
import java.util.*

private val logger = LoggerFactory.getLogger("VedtakRoutes")

fun Routing.vedtak(arenaoppslagRestClient: ArenaoppslagRestClient) {
    //authenticate {
        post("/fellesordning/vedtak", {
            securitySchemeNames = setOf("Maskinporten")
            response {
                HttpStatusCode.OK to {
                    description = "Henter et vedtak"
                    body<IverksettVedtakKafkaDto> {}
                }
            }
        }) {
            try {
                logger.info("Incomming")
                val request = call.receive<ArenaoppslagRequest>()
                call.respond(arenaoppslagRestClient.hentVedtak(request))
            } catch(e: Exception) {
                logger.error("Feil i kall", e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    //}
}
