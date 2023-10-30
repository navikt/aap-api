package api.fellesordningen

import api.arena.ArenaoppslagRestClient
import api.util.fellesordningenCallCounter
import api.util.fellesordningenCallFailedCounter
import api.sporingslogg.SporingsloggEntry
import api.sporingslogg.SporingsloggKafkaClient
import api.util.Config
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.LocalDateTime
import java.util.*

private val secureLog = LoggerFactory.getLogger("secureLog")
private val logger = LoggerFactory.getLogger("App")
private const val ORGNR = "987414502"
private val objectMapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .registerModule(JavaTimeModule())

fun Route.fellesordningen(
    config: Config,
    arenaoppslagRestClient: ArenaoppslagRestClient,
    sporingsloggKafkaClient: SporingsloggKafkaClient
) {
    post("/fellesordning/vedtak") {
        fellesordningenCallCounter.inc()
        val body = call.receive<VedtakRequest>()
        val callId = requireNotNull(call.request.header("x-callid")) { "x-callid ikke satt" }
        runCatching {
            arenaoppslagRestClient.hentVedtak(UUID.fromString(callId), body)
        }.onFailure { ex ->
            fellesordningenCallFailedCounter.inc()
            secureLog.error("Klarte ikke hente vedtak fra Arena", ex)
            throw ex
        }.onSuccess { res ->
            if (config.sporingslogg.enabled) {
                try {
                    sporingsloggKafkaClient.sendMelding(lagSporingsloggEntry(body.personId, res))
                    call.respond(res)
                } catch (e: Exception) {
                    secureLog.error("Klarte ikke produsere til kafka sporingslogg og kan derfor ikke returnere data", e)
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        "Feilet sporing av oppslag, kan derfor ikke returnere data. Feilen er på vår side, prøv igjen senere."
                    )
                }
            } else {
                call.respond(res)
            }
        }
    }
}

private fun lagSporingsloggEntry(
    person: String,
    leverteData: Any
) = SporingsloggEntry(
    person = person,
    mottaker = ORGNR,
    tema = "AAP",
    behandlingsGrunnlag = "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
    uthentingsTidspunkt = LocalDateTime.now(),
    leverteData = Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(leverteData).encodeToByteArray())
)

