package api.afp

import api.Maksimum2
import api.arena.ArenaoppslagRestClient
import api.auth.MASKINPORTEN_AFP_OFFENTLIG
import api.auth.MASKINPORTEN_AFP_PRIVAT
import api.sporingslogg.SporingsloggKafkaClient
import api.auth.hentConsumerId
import api.sporingslogg.Spor
import api.sporingslogg.SporingsloggException
import api.util.Consumers.getConsumerTag
import api.util.httpCallCounter
import api.util.httpFailedCallCounter
import api.util.sporingsloggFailCounter
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*

private val secureLog = LoggerFactory.getLogger("secureLog")
private val logger = LoggerFactory.getLogger("App")

fun Route.afp(
    brukSporingslogg: Boolean,
    arenaoppslagRestClient: ArenaoppslagRestClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {

    route("/afp") {
        authenticate(MASKINPORTEN_AFP_PRIVAT) {
            post("/fellesordningen") {
                hentPerioder(call, brukSporingslogg, arenaoppslagRestClient, sporingsloggClient, prometheus)
            }
        }

        authenticate(MASKINPORTEN_AFP_OFFENTLIG) {
            post("/offentlig") {
                hentPerioder(call, brukSporingslogg, arenaoppslagRestClient, sporingsloggClient, prometheus)
            }
        }

        post("/test"){
            hentMaksimumTest(call.receive(), arenaoppslagRestClient)
        }

    }
}

fun hentMaksimumTest(vedtakRequest: VedtakRequest, arenaoppslagRestClient: ArenaoppslagRestClient): Maksimum2 {
    return arenaoppslagRestClient.hentMaksimumTest(vedtakRequest)
}

private suspend fun hentPerioder(
    call: ApplicationCall,
    brukSporingslogg: Boolean,
    arenaoppslagRestClient: ArenaoppslagRestClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {
    val orgnr = call.hentConsumerId()
    val consumerTag = getConsumerTag(orgnr)

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
        if (brukSporingslogg) {
            try {
                sporingsloggClient.send(Spor.opprett(body.personidentifikator, res, orgnr))
                call.respond(res)
            } catch (e: Exception) {
                prometheus.sporingsloggFailCounter(consumerTag).increment()
                throw SporingsloggException(e)
            }
        } else {
            logger.warn("Sporingslogg er skrudd av, returnerer data uten å sende til kafka")
            call.respond(res)
        }
    }
}
