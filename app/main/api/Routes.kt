package api

import api.afp.VedtakPeriode
import api.afp.VedtakRequest
import api.afp.VedtakRequestMedSaksRef
import api.afp.VedtakResponse
import api.arena.IArenaoppslagRestClient
import api.auth.MASKINPORTEN_AFP_OFFENTLIG
import api.auth.MASKINPORTEN_AFP_PRIVAT
import api.auth.MASKINPORTEN_TP_ORDNINGEN
import api.auth.hentConsumerId
import api.sporingslogg.Spor
import api.sporingslogg.SporingsloggException
import api.sporingslogg.SporingsloggKafkaClient
import api.tp.ITpRegisterClient
import api.util.Consumers.getConsumerTag
import api.util.httpCallCounter
import api.util.httpFailedCallCounter
import api.util.sporingsloggFailCounter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import org.slf4j.LoggerFactory
import java.util.*

private val secureLog = LoggerFactory.getLogger("secureLog")
private val logger = LoggerFactory.getLogger("App")

fun Route.api(
    brukSporingslogg: Boolean,
    arenaoppslagRestClient: IArenaoppslagRestClient,
    sporingsloggClient: SporingsloggKafkaClient,
    tpRegisterClient: ITpRegisterClient,
    prometheus: PrometheusMeterRegistry
) {

    route("/afp") {
        authenticate(MASKINPORTEN_AFP_PRIVAT) {
            post("/fellesordningen") {
                hentPerioder(
                    call,
                    brukSporingslogg,
                    arenaoppslagRestClient,
                    sporingsloggClient,
                    prometheus
                )
            }
        }

        authenticate(MASKINPORTEN_AFP_OFFENTLIG) {
            post("/offentlig") {
                hentPerioder(
                    call,
                    brukSporingslogg,
                    arenaoppslagRestClient,
                    sporingsloggClient,
                    prometheus
                )
            }
        }
    }
    route("/tp-samhandling-med-utbetalinger") {
        authenticate(MASKINPORTEN_TP_ORDNINGEN) {
            post {
                val body = call.receive<VedtakRequest>()
                if (tpRegisterClient.brukerHarTpForholdOgYtelse(
                        body.personidentifikator,
                        call.hentConsumerId(),
                        call.callId ?: UUID.randomUUID().toString()
                    ) != true
                ) {
                    call.respond(HttpStatusCode.NotFound, "Mangler TP-ytelse.")
                } else {
                    call.respond(
                        hentMaksimum(
                            call,
                            body,
                            brukSporingslogg,
                            arenaoppslagRestClient,
                            sporingsloggClient,
                            prometheus
                        )
                    )
                }
            }
        }
    }

    route("/tp-samhandling") {
        authenticate(MASKINPORTEN_TP_ORDNINGEN) {
            post {
                val body = call.receive<VedtakRequest>()
                if (tpRegisterClient.brukerHarTpForholdOgYtelse(
                        body.personidentifikator,
                        call.hentConsumerId(),
                        call.callId ?: UUID.randomUUID().toString()
                    ) != true
                ) {
                    call.respond(HttpStatusCode.NotFound, "Mangler TP-ytelse.")
                } else {
                    call.respond(
                        hentMedium(
                            call,
                            body,
                            brukSporingslogg,
                            arenaoppslagRestClient,
                            sporingsloggClient,
                            prometheus
                        )
                    )
                }
            }
        }
    }

    if (Miljø.er() == MiljøKode.DEV) {
        route("/tp-samhandling-2") {
            authenticate(MASKINPORTEN_TP_ORDNINGEN) {
                post {
                    val body = call.receive<VedtakRequest>()
                    if (tpRegisterClient.brukerHarTpForholdOgYtelse(
                            body.personidentifikator,
                            982759412.toString(),
                            call.callId ?: UUID.randomUUID().toString()
                        ) != true
                    ) {
                        call.respond(HttpStatusCode.NotFound, "Mangler TP-ytelse.")
                    } else {
                        call.respond(
                            hentMedium(
                                call,
                                body,
                                brukSporingslogg,
                                arenaoppslagRestClient,
                                sporingsloggClient,
                                prometheus
                            )
                        )
                    }
                }
            }
        }
    }
}

private suspend fun hentPerioder(
    call: ApplicationCall,
    brukSporingslogg: Boolean,
    arenaoppslagRestClient: IArenaoppslagRestClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {
    val orgnr = call.hentConsumerId()
    val consumerTag = getConsumerTag(orgnr)

    prometheus.httpCallCounter(consumerTag, call.request.path()).increment()
    val body = call.receive<VedtakRequestMedSaksRef>()
    val callId = requireNotNull(call.request.header("x-callid")) { "x-callid ikke satt" }
    runCatching {
        VedtakResponse(
            perioder = arenaoppslagRestClient.hentVedtakFellesordning(
                UUID.fromString(callId),
                body.toVedtakRequest()
            ).perioder.map { VedtakPeriode(it.fraOgMedDato, it.tilOgMedDato) })
    }.onFailure { ex ->
        prometheus.httpFailedCallCounter(consumerTag, call.request.path()).increment()
        secureLog.error("Klarte ikke hente vedtak fra Arena", ex)
        logger.error("Klarte ikke hente vedtak fra Arena. Se sikker logg for stacktrace.")
        throw ex
    }.onSuccess { res ->
        if (brukSporingslogg) {
            try {
                sporingsloggClient.send(
                    Spor.opprett(
                        body.personidentifikator,
                        res,
                        orgnr
                    )
                )
                call.respond(res)
            } catch (e: Exception) {
                prometheus.sporingsloggFailCounter(consumerTag).increment()
                throw SporingsloggException(e)
            }
        } else {
            logger.info("Sporingslogg er skrudd av, returnerer data uten å sende til Kafka.")
            call.respond(res)
        }
    }
}

private suspend fun hentMedium(
    call: ApplicationCall,
    body: VedtakRequest,
    brukSporingslogg: Boolean,
    arenaoppslagRestClient: IArenaoppslagRestClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {
    val orgnr = call.hentConsumerId()
    val consumerTag = getConsumerTag(orgnr)

    prometheus.httpCallCounter(consumerTag, call.request.path()).increment()
    val callId = requireNotNull(call.request.header("x-callid")) { "x-callid ikke satt" }
    runCatching {
        val arenaOppslagRequestBody = EksternVedtakRequest(
            personidentifikator = body.personidentifikator,
            fraOgMedDato = body.fraOgMedDato,
            tilOgMedDato = body.tilOgMedDato
        )
        arenaoppslagRestClient.hentMaksimum(callId, arenaOppslagRequestBody)
            .fraKontraktUtenUtbetalinger()
    }.onFailure { ex ->
        prometheus.httpFailedCallCounter(consumerTag, call.request.path()).increment()
        secureLog.error("Klarte ikke hente vedtak fra Arena", ex)
        logger.error("Klarte ikke hente vedtak fra Arena. Se sikker logg for stacktrace.")
        throw ex
    }.onSuccess { res ->
        if (brukSporingslogg) {
            try {
                sporingsloggClient.send(
                    Spor.opprett(
                        body.personidentifikator,
                        res,
                        orgnr
                    )
                )
                call.respond(res)
            } catch (e: Exception) {
                prometheus.sporingsloggFailCounter(consumerTag).increment()
                throw SporingsloggException(e)
            }
        } else {
            logger.info("Sporingslogg er skrudd av, returnerer data uten å sende til Kafka.")
            call.respond(res)
        }
    }
}


private suspend fun hentMaksimum(
    call: ApplicationCall,
    body: VedtakRequest,
    brukSporingslogg: Boolean,
    arenaoppslagRestClient: IArenaoppslagRestClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {
    val orgnr = call.hentConsumerId()
    val consumerTag = getConsumerTag(orgnr)

    prometheus.httpCallCounter(consumerTag, call.request.path()).increment()
    val callId = requireNotNull(call.request.header("x-callid")) { "x-callid ikke satt" }
    runCatching {
        val arenaOppslagRequestBody = EksternVedtakRequest(
            personidentifikator = body.personidentifikator,
            fraOgMedDato = body.fraOgMedDato,
            tilOgMedDato = body.tilOgMedDato
        )
        arenaoppslagRestClient.hentMaksimum(callId, arenaOppslagRequestBody).fraKontrakt()
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
            logger.info("Sporingslogg er skrudd av, returnerer data uten å sende til Kafka.")
            call.respond(res)
        }
    }
}

