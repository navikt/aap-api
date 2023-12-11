package api.fellesordningen

import api.arena.ArenaoppslagRestClient
import api.sporingslogg.Spor
import api.sporingslogg.SporingsloggKafkaClient
import api.util.Config
import api.util.httpCallCounter
import api.util.httpFailedCallCounter
import api.util.sporingsloggFailCounter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*

private val secureLog = LoggerFactory.getLogger("secureLog")
private val logger = LoggerFactory.getLogger("App")
private const val consumerTag = "fellesordningen"

fun Route.fellesordningen(
    config: Config,
    arenaoppslagRestClient: ArenaoppslagRestClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {
    post("/fellesordning-for-afp") {
        prometheus.httpCallCounter(consumerTag, call.request.path()).increment()
        val body = call.receive<VedtakRequest>()
        val callId = requireNotNull(call.request.header("x-callid")) { "x-callid ikke satt" }
        runCatching {
            arenaoppslagRestClient.hentVedtakFellesordning(UUID.fromString(callId), body)
        }.onFailure { ex ->
            prometheus.httpFailedCallCounter(consumerTag, call.request.path()).increment()
            secureLog.error("Klarte ikke hente vedtak fra Arena", ex)
            throw ex
        }.onSuccess { res ->
            if (config.sporingslogg.enabled) {
                try {
                    sporingsloggClient.send(Spor.opprett(body.personidentifikator, res))
                    call.respond(res)
                } catch (e: Exception) {
                    prometheus.sporingsloggFailCounter(consumerTag).increment()
                    secureLog.error("Klarte ikke produsere til kafka sporingslogg og kan derfor ikke returnere data", e)
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        "Feilet sporing av oppslag, kan derfor ikke returnere data. Feilen er på vår side, prøv igjen senere."
                    )
                }
            } else {
                logger.warn("Sporingslogg er skrudd av, returnerer data uten å sende til kafka")
                call.respond(res)
            }
        }
    }
}


