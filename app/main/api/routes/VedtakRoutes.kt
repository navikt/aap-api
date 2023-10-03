package api.routes

import api.Config
import api.arena.ArenaoppslagRequest
import api.arena.ArenaoppslagResponse
import api.arena.ArenaoppslagRestClient
import api.auth.MASKINPORTEN_AUTH_NAME
import api.auth.SAMTYKKE_AUTH_NAME
import api.auth.verifyJwt
import io.github.smiley4.ktorswaggerui.dsl.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.lang.Exception


private val logger = LoggerFactory.getLogger("VedtakRoutes")

fun Routing.vedtak(arenaoppslagRestClient: ArenaoppslagRestClient, config: Config) {
    authenticate(MASKINPORTEN_AUTH_NAME) {
        post("/fellesordning/vedtak", {
            securitySchemeNames = setOf("Maskinporten")
            response {
                HttpStatusCode.OK to {
                    description = "Henter et vedtak"
                    body<ArenaoppslagResponse> {}
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
        get("/dsop/test") {
            val principal = call.principal<JWTPrincipal>()
            val token = principal?.payload?.getClaim("consumer")
            logger.info("Token: {$token}")
            if(verifyJwt(requireNotNull(call.request.header("NAV-samtykke-token")), config)) {
                call.respond(HttpStatusCode.Forbidden)
            }
            call.respond("OK")
        }

    }

}
