package api.util

import api.auth.SamtykkeIkkeGittException
import api.getCallId
import api.sporingslogg.SporingsloggException
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import org.slf4j.Logger

data class FeilRespons(
    val melding: String,
)

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
                    FeilRespons("Feilet sporing av oppslag, kan derfor ikke returnere data. Feilen er på vår side, prøv igjen senere. x-call-id: ${call.getCallId()}")
                )
            }

            is SamtykkeIkkeGittException -> {
                logger.warn("Samtykke ikke gitt. x-call-id: ${call.getCallId()}", cause)
                call.respond(HttpStatusCode.Forbidden, FeilRespons("Samtykke ikke gitt"))
            }

            is ContentTransformationException -> {
                logger.warn("Feil i mottatte data: ContentTransformationException", cause)
                call.respond(HttpStatusCode.BadRequest, "Feil i mottatte data")
            }

            is PeriodeErrorException -> {
                logger.warn("Feil i periode:", rootCause)
                call.respond(HttpStatusCode.BadRequest, "${rootCause.message}")
            }

            is IllegalArgumentException -> {
                logger.warn("Feil i mottatte data: IllegalArgumentException. x-call-id: ${call.getCallId()}", cause)
                call.respond(HttpStatusCode.BadRequest, "Feil i mottatte data")
            }

            is ManglerTilgangException -> {
                call.respond(
                    HttpStatusCode.Forbidden,
                    FeilRespons("Mangler tilgang til baksysystemer. x-call-id: ${call.getCallId()}")
                )
            }

            is BadRequestException -> {
                logger.info("Bad request. Message: ${rootCause.message}")
                call.respond(HttpStatusCode.BadRequest, FeilRespons("Feil i mottatte data. x-call-id: ${call.getCallId()}"))
            }

            else -> {
                if (cause is BadRequestException) {
                    logger.warn("Bad request. Melding: ${cause.message}", cause)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        FeilRespons("Feil i mottatte data: ${rootCause.message}. x-call-id: ${call.getCallId()}")
                    )
                    return@exception
                }
                logger.error(
                    "Uhåndtert feil ved kall mot ${call.request.path()}. Feiltype: ${cause.javaClass}. x-call-id: ${call.getCallId()}.",
                    cause
                )
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Feil i tjeneste: ${cause.message}. x-call-id: ${call.getCallId()} "
                )
            }
        }
        prometheusMeterRegistry.uhåndtertExceptionCounter(cause.javaClass.simpleName)
    }
}

class PeriodeErrorException(message: String) : Exception(message)