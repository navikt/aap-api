package api.util

import api.auth.SamtykkeIkkeGittException
import api.getCallId
import api.sporingslogg.SporingsloggException
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.response.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import org.slf4j.Logger

sealed class FeilRespons {
    abstract val melding: String
    abstract val kode: String
}

data class ManglerTilgangFeil(
    override val melding: String,
    override val kode: String
) : FeilRespons()

data class IkkeFunnetFeil(
    override val melding: String,
    override val kode: String
) : FeilRespons()

data class UgyldigForespørselFeil(
    override val melding: String,
    override val kode: String
) : FeilRespons()

data class UautorisertFeil(
    override val melding: String,
    override val kode: String
) : FeilRespons()

data class InternFeil(
    override val melding: String,
    override val kode: String
) : FeilRespons()

private fun Throwable.findRootCause(): Throwable =
    generateSequence(this) { it.cause }
        .takeWhile { it.cause != it }
        .last()

fun StatusPagesConfig.feilhåndtering(
    logger: Logger,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    exception<Throwable> { call, cause ->
        when (val rootCause = cause.findRootCause()) {
            is SporingsloggException -> {
                logger.error(
                    "Klarte ikke produsere til Kafka sporingslogg og kan derfor ikke returnere data, x-call-id: ${call.getCallId()}",
                    cause
                )
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    InternFeil(
                        "Feilet sporing av oppslag, kan derfor ikke returnere data. Feilen er på vår side, prøv igjen senere. x-call-id: ${call.getCallId()}",
                        "FEIL_I_SPORINGSLOGG"
                    )
                )
            }

            is SamtykkeIkkeGittException -> {
                logger.warn("Samtykke ikke gitt. x-call-id: ${call.getCallId()}", cause)
                call.respond(
                    HttpStatusCode.Forbidden,
                    ManglerTilgangFeil("Samtykke ikke gitt.", "SAMTYKKE_IKKE_GITT")
                )
            }

            is ContentTransformationException -> {
                logger.warn("Feil i mottatte data.", cause)
                call.respond(
                    HttpStatusCode.BadRequest,
                    UgyldigForespørselFeil("Feil i mottatte data.", "FEIL_I_DATA")
                )
            }

            is PeriodeErrorException -> {
                logger.warn("Feil i periode:", rootCause)
                call.respond(
                    HttpStatusCode.BadRequest,
                    UgyldigForespørselFeil("${rootCause.message}", "FEIL_I_PERIODE")
                )
            }

            is IllegalArgumentException -> {
                logger.warn("Feil i mottatte data.. x-call-id: ${call.getCallId()}", cause)
                call.respond(
                    HttpStatusCode.BadRequest,
                    UgyldigForespørselFeil("Feil i mottatte data", "FEIL_I_DATA")
                )
            }

            is ManglerTilgangException -> {
                logger.warn("Mangler tilgang til bruker. x-call-id: ${call.getCallId()}", cause)
                call.respond(
                    HttpStatusCode.Forbidden,
                    ManglerTilgangFeil(
                        "Mangler tilgang til baksysystemer. x-call-id: ${call.getCallId()}",
                        "MANGLER_TILGANG"
                    )
                )
            }

            is BadRequestException -> {
                logger.info("Bad request. Message: ${rootCause.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    UgyldigForespørselFeil("Feil i mottatte data", "FEIL_I_DATA")
                )
            }

            else -> {
                if (cause is BadRequestException) {
                    logger.warn("Bad request. Melding: ${cause.message}", cause)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        UgyldigForespørselFeil("Feil i mottatte data", "FEIL_I_DATA")
                    )
                    return@exception
                }
                logger.error(
                    "Uhåndtert feil ved kall mot ${call.request.path()}. Feiltype: ${cause.javaClass}. x-call-id: ${call.getCallId()}.",
                    cause
                )
                call.respond(
                    HttpStatusCode.InternalServerError,
                    InternFeil(
                        "Feil i tjeneste: ${cause.message}. x-call-id: ${call.getCallId()}",
                        "UKJENT_FEIL"
                    )
                )
            }
        }
        prometheusMeterRegistry.uhåndtertExceptionCounter(cause.javaClass.simpleName)
    }
}

class PeriodeErrorException(message: String) : Exception(message)