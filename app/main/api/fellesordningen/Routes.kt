package api.fellesordningen

import api.arena.ArenaoppslagRestClient
import api.fellesordningenCallCounter
import api.fellesordningenCallFailedCounter
import api.sporingslogg.SporingsloggKafkaClient
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("FellesordningenRoutes")

fun Route.fellesordningen(
    arenaoppslagRestClient: ArenaoppslagRestClient,
    sporingsloggKafkaClient: SporingsloggKafkaClient
) {
    post("/fellesordning/vedtak") {
        fellesordningenCallCounter.inc()
        val body = call.receive<VedtakRequest>()
        val callId = requireNotNull(call.request.header("x-callid")){"x-callid ikke satt"}
        runCatching {
            arenaoppslagRestClient.hentVedtak(UUID.fromString(callId), body)
        }.onFailure { ex ->
            fellesordningenCallFailedCounter.inc()
            logger.error("Feil i kall mot hentVedtak", ex)
            throw ex
        }.onSuccess { res ->
            call.respond(res)
        }
    }
}

