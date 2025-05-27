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
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.UUID
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("App")

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
    val callId = requireNotNull(call.request.header("x-callid")) { "x-callid ikke satt" }
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
        if (brukSporingslogg) {
            try {
                sporingsloggClient.send(
                    Spor.opprett(
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
        if (brukSporingslogg) {
            try {
                sporingsloggClient.send(
                    Spor.opprett(
                        personIdent = body.personidentifikator,
                        utlevertData = res,
                        requestObjekt = body,
                        konsumentOrgNr = orgnr,
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
    val callId = requireNotNull(call.request.header("x-callid")) { "x-callid ikke satt" }
    runCatching {
        val arenaOppslagRequestBody = EksternVedtakRequest(
            personidentifikator = body.personidentifikator,
            fraOgMedDato = body.fraOgMedDato,
            tilOgMedDato = body.tilOgMedDato
        )
        val res = apiInternClient.hentMaksimum(callId, arenaOppslagRequestBody)
        Maksimum(
            vedtak = res.vedtak.map {
                Vedtak(
                    dagsats = it.dagsats,
                    vedtakId = it.vedtakId,
                    status = it.status,
                    saksnummer = it.saksnummer,
                    vedtaksdato = localDate(it.vedtaksdato.toString()),
                    vedtaksTypeKode =  it.vedtaksTypeKode,
                    periode = Periode(it.periode.fraOgMedDato, it.periode.tilOgMedDato),
                    rettighetsType = it.rettighetsType,
                    beregningsgrunnlag = it.beregningsgrunnlag,
                    barnMedStonad = it.barnMedStonad,
                    kildesystem = it.kildesystem.toString(),
                    samordningsId = it.samordningsId,
                    opphorsAarsak = it.opphorsAarsak,
                    vedtaksTypeNavn = it.vedtaksTypeNavn,
                    utbetaling = it.utbetaling.map {
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
                            barnetilegg = it.barnetilegg
                        )
                    },
                )
            }.filter { it.status != "INAKT" })
    }.onFailure { ex ->
        prometheus.httpFailedCallCounter(consumerTag, call.request.path()).increment()
        logger.error("Klarte ikke hente vedtak fra intern API", ex)
        throw ex
    }.onSuccess { res ->
        if (brukSporingslogg) {
            try {
                sporingsloggClient.send(Spor.opprett(
                    personIdent = body.personidentifikator,
                    utlevertData = res,
                    requestObjekt = body,
                    konsumentOrgNr = orgnr,
                ))
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

