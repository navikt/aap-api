package api.dsop

import api.auth.verifiserOgPakkUtSamtykkeToken
import api.getCallId
import api.sporingslogg.Spor
import api.sporingslogg.SporingsloggKafkaClient
import api.util.Config
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


private val logger = LoggerFactory.getLogger("App")
private val secureLog = LoggerFactory.getLogger("secureLog")

fun Routing.dsop(
    config: Config,
    sporingsloggKafkaClient: SporingsloggKafkaClient
) {
    post("/dsop/meldeplikt") {//TODO: gjør om til get()
        val callId = requireNotNull(call.getCallId()) { "x-callid ikke satt" }
        val samtykke = verifiserOgPakkUtSamtykkeToken(requireNotNull(call.request.header("NAV-samtykke-token")), call, config)
        val dsopRequest = call.receive<DsopRequest>()
        logger.info("Samtykke OK: ${samtykke.samtykkeperiode}")
        try {
//            arenaoppslagRestClient.hentMeldepliktDsop(UUID.fromString(callId),dsopRequest)
            sporingsloggKafkaClient.send(
                Spor(
                    samtykke.personIdent,
                    samtykke.consumerId,
                    "aap",
                    "behandlingsgrunnlag",
                    LocalDateTime.now(),
                    "leverteData",
                    samtykke.samtykketoken,
                    "dataForespoersel",
                    "leverandoer"
                )
            )
            call.respond("OK")
        } catch (e:Exception){
            secureLog.error("Klarte ikke produsere til kafka sporingslogg og kan derfor ikke returnere data", e)
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                "Feilet sporing av oppslag, kan derfor ikke returnere data. Feilen er på vår side, prøv igjen senere."
            )
        }
    }
    post("/dsop/vedtak") {//TODO: gjør om til get()
        val callId = requireNotNull(call.getCallId()) { "x-callid ikke satt" }
        val samtykke = verifiserOgPakkUtSamtykkeToken(requireNotNull(call.request.header("NAV-samtykke-token")), call, config)
        val dsopRequest = call.receive<DsopRequest>() //TODO: hent ut personId fra token
        logger.info("Samtykke OK: ${samtykke.samtykkeperiode}")
        try {
//            arenaoppslagRestClient.hentMeldepliktDsop(UUID.fromString(callId),dsopRequest)
            sporingsloggKafkaClient.send(
                Spor(
                    samtykke.personIdent,
                    samtykke.consumerId,
                    "aap",
                    "behandlingsgrunnlag",
                    LocalDateTime.now(),
                    "leverteData",
                    samtykke.samtykketoken,
                    "dataForespoersel",
                    "leverandoer"
                )
            )
            call.respond("OK")
        } catch (e:Exception){
            secureLog.error("Klarte ikke produsere til kafka sporingslogg og kan derfor ikke returnere data", e)
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                "Feilet sporing av oppslag, kan derfor ikke returnere data. Feilen er på vår side, prøv igjen senere."
            )
        }
    }
}