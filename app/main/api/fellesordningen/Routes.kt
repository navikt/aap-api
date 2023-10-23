package api.fellesordningen

import api.arena.ArenaoppslagRestClient
import api.util.fellesordningenCallCounter
import api.util.fellesordningenCallFailedCounter
import api.sporingslogg.SporingsloggEntry
import api.sporingslogg.SporingsloggKafkaClient
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.LocalDateTime
import java.util.*

private val logger = LoggerFactory.getLogger("FellesordningenRoutes")
private const val ORGNR = "987414502"
private val objectMapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .registerModule(JavaTimeModule())

fun Route.fellesordningen(
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
            logger.error("Feil i kall mot hentVedtak", ex)
            throw ex
        }.onSuccess { res ->
            // TODO Gjør dette akkurat nå for at denne skal overleve i testfase.
            //      Når vi er klare så skal vi ikke returnere data dersom vi ikke
            //      klarer å poste til Kafka
            try {
                sporingsloggKafkaClient.sendMelding(lagSporingsloggEntry(body.personId, res))
            } catch (e: Exception) {
                logger.error("Feilet å poste til Kafka", e)
            }
            call.respond(res)
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
    behandlingsGrunnlag = "Hjemmel?",
    uthentingsTidspunkt = LocalDateTime.now(),
    leverteData = Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(leverteData).encodeToByteArray())
)

