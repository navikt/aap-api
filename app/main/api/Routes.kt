package api

import api.afp.VedtakPeriode
import api.afp.VedtakRequest
import api.afp.VedtakRequestMedSaksRef
import api.afp.VedtakResponse
import api.api_intern.IApiInternClient
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
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.*

private val logger = LoggerFactory.getLogger("App")

fun ApplicationCall.getCallId(): String? {
    val headerNames = listOf("x-call-id", "x-callid", "x-request-id")
    return headerNames.firstNotNullOfOrNull { request.headers[it] }
}

fun Route.api(
    brukSporingslogg: Boolean,
    apiInternClient: IApiInternClient,
    sporingsloggClient: SporingsloggKafkaClient,
    tpRegisterClient: ITpRegisterClient,
    prometheus: PrometheusMeterRegistry
) {

    route("/afp") {
        authenticate(MASKINPORTEN_AFP_PRIVAT) {
            post("/fellesordningen") {
                val body = call.receive<VedtakRequest>()
                hentPerioder(
                    call,
                    body,
                    saksnummer = null,
                    brukSporingslogg,
                    apiInternClient,
                    sporingsloggClient,
                    prometheus
                )
            }
        }

        authenticate(MASKINPORTEN_AFP_OFFENTLIG) {
            post("/offentlig") {
                val body = call.receive<VedtakRequestMedSaksRef>()
                hentPerioder(
                    call,
                    body.tilVedtakRequest(),
                    saksnummer = body.saksId,
                    brukSporingslogg,
                    apiInternClient,
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
                            apiInternClient,
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
                            apiInternClient,
                            sporingsloggClient,
                            prometheus
                        )
                    )
                }
            }
        }
    }/* TESTING PURPOSES
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
                                hentMaksimum(
                                    call,
                                    body,
                                    brukSporingslogg,
                                    apiInternClient,
                                    sporingsloggClient,
                                    prometheus
                                )
                            )
                        }
                    }
                }
            }
        }*/
}

private suspend fun hentPerioder(
    call: ApplicationCall,
    vedtakRequest: VedtakRequest,
    saksnummer: String? = null,
    brukSporingslogg: Boolean,
    apiInternClient: IApiInternClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {
    val orgnr = call.hentConsumerId()
    val consumerTag = getConsumerTag(orgnr)

    prometheus.httpCallCounter(consumerTag, call.request.path()).increment()
    val callId = requireNotNull(call.getCallId()) { "x-callid ikke satt" }
    runCatching {
        VedtakResponse(
            perioder = apiInternClient.hentPerioder(
                UUID.fromString(callId),
                vedtakRequest
            ).perioder.map { VedtakPeriode(it.fraOgMedDato, it.tilOgMedDato) }
        )
    }.onFailure { ex ->
        prometheus.httpFailedCallCounter(consumerTag, call.request.path()).increment()
        logger.error("Klarte ikke hente vedtak fra intern API", ex)
        throw ex
    }.onSuccess { res ->
        sporingsloggClient.loggTilSporingslogg(
            brukSporingslogg = brukSporingslogg,
            prometheus = prometheus,
            consumerTag = consumerTag,
            spor = Spor.opprett(
                personIdent = vedtakRequest.personidentifikator,
                utlevertData = res,
                konsumentOrgNr = orgnr,
                requestObjekt = if (saksnummer != null) VedtakRequestMedSaksRef(
                    personidentifikator = vedtakRequest.personidentifikator,
                    fraOgMedDato = vedtakRequest.fraOgMedDato,
                    tilOgMedDato = vedtakRequest.tilOgMedDato,
                    saksId = saksnummer
                ) else vedtakRequest,
            )
        )
        call.respond(res)
    }
}

private suspend fun hentMedium(
    call: ApplicationCall,
    body: VedtakRequest,
    brukSporingslogg: Boolean,
    apiInternClient: IApiInternClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {
    val orgnr = call.hentConsumerId()
    val consumerTag = getConsumerTag(orgnr)

    prometheus.httpCallCounter(consumerTag, call.request.path()).increment()

    runCatching {
        val arenaOppslagRequestBody = EksternVedtakRequest(
            personidentifikator = body.personidentifikator,
            fraOgMedDato = body.fraOgMedDato,
            tilOgMedDato = body.tilOgMedDato
        )
        val res = apiInternClient.hentMedium(arenaOppslagRequestBody)
        Medium(
            vedtak = res.vedtak.map {
                VedtakUtenUtbetaling(
                    dagsats = it.dagsats,
                    vedtakId = it.vedtakId,
                    status = it.status,
                    saksnummer = it.saksnummer,
                    vedtaksdato = localDate(it.vedtaksdato.toString()),
                    vedtaksTypeKode = it.vedtaksTypeKode,
                    vedtaksTypeNavn = it.vedtaksTypeNavn,
                    periode = Periode(it.periode.fraOgMedDato, it.periode.tilOgMedDato),
                    rettighetsType = it.rettighetsType,
                    beregningsgrunnlag = it.beregningsgrunnlag,
                    barnMedStonad = it.barnMedStonad,
                    barnetillegg = it.barnetillegg * it.barnMedStonad,
                    kildesystem = it.kildesystem,
                    samordningsId = it.samordningsId,
                    opphorsAarsak = it.opphorsAarsak
                )
            }.filter { it.status != "INAKT" }
        )
    }.onFailure { ex ->
        prometheus.httpFailedCallCounter(consumerTag, call.request.path()).increment()
        logger.error("Klarte ikke hente vedtak fra intern API", ex)
        throw ex
    }.onSuccess { res ->
        sporingsloggClient.loggTilSporingslogg(
            brukSporingslogg,
            prometheus,
            consumerTag,
            Spor.opprett(
                personIdent = body.personidentifikator,
                utlevertData = res,
                requestObjekt = body,
                konsumentOrgNr = orgnr,
            )
        )
        call.respond(res)
    }
}

private fun SporingsloggKafkaClient.loggTilSporingslogg(
    brukSporingslogg: Boolean,
    prometheus: PrometheusMeterRegistry,
    consumerTag: String,
    spor: Spor
) {
    if (brukSporingslogg) {
        try {
            this.send(spor)
        } catch (e: Exception) {
            prometheus.sporingsloggFailCounter(consumerTag).increment()
            throw SporingsloggException(e)
        }
    } else {
        logger.info("Sporingslogg er skrudd av, returnerer data uten å sende til Kafka.")
    }
}

fun localDate(s: String): LocalDate {
    val formatter = DateTimeFormatterBuilder()
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        .toFormatter()

    return try {
        LocalDate.parse(s, formatter)
    } catch (e: DateTimeParseException) {
        logger.error("Failed to parse date string: $s", e)
        throw e
    }
}


private suspend fun hentMaksimum(
    call: ApplicationCall,
    body: VedtakRequest,
    brukSporingslogg: Boolean,
    apiInternClient: IApiInternClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {
    val orgnr = call.hentConsumerId()
    val consumerTag = getConsumerTag(orgnr)

    prometheus.httpCallCounter(consumerTag, call.request.path()).increment()
    val callId = requireNotNull(call.getCallId()) { "x-callid ikke satt" }
    runCatching {
        val arenaOppslagRequestBody = EksternVedtakRequest(
            personidentifikator = body.personidentifikator,
            fraOgMedDato = body.fraOgMedDato,
            tilOgMedDato = body.tilOgMedDato
        )
        logger.info("Henter maksimum fra API intern. Fra og med-dato: ${arenaOppslagRequestBody.fraOgMedDato}. Til og med dato: ${arenaOppslagRequestBody.tilOgMedDato}.")
        val maksimumFraApiIntern = apiInternClient.hentMaksimum(callId, arenaOppslagRequestBody)
        Maksimum(
            vedtak = maksimumFraApiIntern.vedtak.map { vedtak ->
                Vedtak(
                    dagsats = vedtak.dagsats,
                    vedtakId = vedtak.vedtakId,
                    status = vedtak.status,
                    saksnummer = vedtak.saksnummer,
                    vedtaksdato = localDate(vedtak.vedtaksdato.toString()),
                    vedtaksTypeKode = vedtak.vedtaksTypeKode,
                    periode = Periode(vedtak.periode.fraOgMedDato, vedtak.periode.tilOgMedDato),
                    rettighetsType = vedtak.rettighetsType,
                    beregningsgrunnlag = vedtak.beregningsgrunnlag,
                    barnMedStonad = vedtak.barnMedStonad,
                    barnetillegg = vedtak.barnetillegg,
                    kildesystem = vedtak.kildesystem.toString(),
                    samordningsId = vedtak.samordningsId,
                    opphorsAarsak = vedtak.opphorsAarsak,
                    vedtaksTypeNavn = vedtak.vedtaksTypeNavn,
                    utbetaling = vedtak.utbetaling.map {
                        UtbetalingMedMer(
                            reduksjon = it.reduksjon?.let {
                                Reduksjon(
                                    timerArbeidet = it.timerArbeidet,
                                    annenReduksjon = it.annenReduksjon
                                )
                            },
                            utbetalingsgrad = it.utbetalingsgrad,
                            periode = Periode(it.periode.fraOgMedDato, it.periode.tilOgMedDato),
                            belop = it.belop,
                            dagsats = it.dagsats,
                            barnetilegg = it.barnetillegg,
                            barnetillegg = it.barnetillegg,
                        )
                    },
                )
            }.filter { it.status != "INAKT" })
    }.onFailure { ex ->
        prometheus.httpFailedCallCounter(consumerTag, call.request.path()).increment()
        logger.error("Klarte ikke hente vedtak fra intern API", ex)
        throw ex
    }.onSuccess { res ->
        sporingsloggClient.loggTilSporingslogg(
            brukSporingslogg = brukSporingslogg,
            prometheus = prometheus,
            consumerTag = consumerTag,
            spor = Spor.opprett(
                personIdent = body.personidentifikator,
                utlevertData = res,
                requestObjekt = body,
                konsumentOrgNr = orgnr,
            )
        )
        call.respond(res)
    }
}

