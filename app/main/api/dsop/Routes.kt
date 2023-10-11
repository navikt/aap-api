package api.dsop

import api.Config
import api.sporingslogg.SporingsloggKafkaClient
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute

fun NormalOpenAPIRoute.dsop(config: Config, sporingsloggKafkaClient: SporingsloggKafkaClient) {

    /*
    get("/dsop/test") {
        val samtykke = verifiserOgPakkUtSamtykkeToken(requireNotNull(call.request.header("NAV-samtykke-token")), call, config)
        logger.info("Samtykke OK: ${samtykke.samtykkeperiode}")
        sporingsloggKafkaClient.sendMelding(SporingsloggEntry(samtykke.personIdent,samtykke.consumerId,"aap", "behandlingsgrunnlag",
            LocalDateTime.now(),"leverteData",samtykke.samtykketoken,"dataForespoersel", "leverandoer"))
        call.respond("OK")
    }
    */
}