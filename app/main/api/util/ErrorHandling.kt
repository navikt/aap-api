package api.util

import api.auth.SamtykkeIkkeGittException
import api.sporingslogg.SporingsloggException
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.Logger

fun StatusPagesConfig.feilhåndtering(logger: Logger, prometheusMeterRegistry: PrometheusMeterRegistry) {
    exception<Throwable> { call, cause ->
        when(cause) {
            is SporingsloggException -> {
                logger.error("Klarte ikke produsere til Kafka sporingslogg og kan derfor ikke returnere data", cause)
                call.respondText(
                    text = "Feilet sporing av oppslag, kan derfor ikke returnere data. Feilen er på vår side, prøv igjen senere.",
                    status = HttpStatusCode.ServiceUnavailable
                )
            }
            is SamtykkeIkkeGittException -> {
                logger.warn("Samtykke ikke gitt", cause)
                call.respondText(text = "Samtykke ikke gitt", status = HttpStatusCode.Forbidden)
            }
            is ContentTransformationException -> {
                logger.warn("Feil i mottatte data", cause)
                call.respondText(text = "Feil i mottatte data", status = HttpStatusCode.BadRequest)
            }
            is IllegalArgumentException -> {
                logger.warn("Feil i mottatte data", cause)
                call.respondText(text = "Feil i mottatte data", status = HttpStatusCode.BadRequest)
            }
            else -> {
                logger.error("Uhåndtert feil", cause)
                call.respondText(text = "Feil i tjeneste: ${cause.message}", status = HttpStatusCode.InternalServerError)
            }
        }
        prometheusMeterRegistry.uhåndtertExceptionCounter(cause.javaClass.simpleName)
    }
}
