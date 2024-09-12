package api

import api.afp.VedtakRequest
import api.arena.ArenaoppslagRestClient
import api.auth.MASKINPORTEN_AFP_OFFENTLIG
import api.auth.MASKINPORTEN_AFP_PRIVAT
import api.auth.MASKINPORTEN_TP_ORDNINGEN
import api.sporingslogg.SporingsloggKafkaClient
import api.auth.hentConsumerId
import api.sporingslogg.Spor
import api.sporingslogg.SporingsloggException
import api.util.Consumers.getConsumerTag
import api.util.httpCallCounter
import api.util.httpFailedCallCounter
import api.util.sporingsloggFailCounter
import io.ktor.http.*
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

fun Route.api(
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
    }
    route("/tp-samhandling"){
        authenticate(MASKINPORTEN_TP_ORDNINGEN) {
            post {
                call.respond(hentMaksimum(call, brukSporingslogg, arenaoppslagRestClient, sporingsloggClient, prometheus))
            }
        }
    }
    //responds with the swagger yaml file
    get("swaggeryaml") {
        call.respondText(
            this::class.java.classLoader.getResource("swagger.yaml")!!.readText(),
            ContentType.parse("application/x-yaml")
        )
    }
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
        logger.error("Klarte ikke hente vedtak fra Arena. Se sikker logg for stacktrace.")
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
            logger.warn("Sporingslogg er skrudd av, returnerer data uten å sende til Kafka.")
            call.respond(res)
        }
    }
}


private suspend fun hentMaksimum(
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
        arenaoppslagRestClient.hentMaksimum(callId, body)
    }.onFailure { ex ->
        prometheus.httpFailedCallCounter(consumerTag, call.request.path()).increment()
        secureLog.error("Klarte ikke hente vedtak fra Arena", ex)
        logger.error("Klarte ikke hente vedtak fra Arena. Se sikker logg for stacktrace.")
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
            logger.warn("Sporingslogg er skrudd av, returnerer data uten å sende til Kafka.")
            call.respond(res)
        }
    }
}

