package api.routes

import api.arena.ArenaoppslagRequest
import api.arena.ArenaoppslagRestClient
import io.github.smiley4.ktorswaggerui.dsl.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.dto.kafka.IverksettVedtakKafkaDto
import no.nav.aap.kafka.streams.v2.StateStore
import java.time.LocalDate
import java.util.*

fun Routing.vedtak(arenaoppslagRestClient: ArenaoppslagRestClient) {
    //authenticate {
        get("/fellesordning/vedtak", {
            securitySchemeNames = setOf("Maskinporten")
            response {
                HttpStatusCode.OK to {
                    description = "Henter et vedtak"
                    body<IverksettVedtakKafkaDto> {}
                }
            }
        }) {
            val request = call.receive<ArenaoppslagRequest>()
            call.respond(arenaoppslagRestClient.hentVedtak(request))
        }
    //}
}
