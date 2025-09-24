package api.sporingslogg

import api.util.Consumers
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

val MAX_SPORINGSLOGG_SIZE = 1_048_576 // 1 MB

class SporingsloggException(cause: Throwable) : Exception(cause)

class SporingsloggKafkaClient(
    private val sporingsloggTopic: String,
    private val kafkaProducer: Producer<String, Spor>
) {
    fun send(spor: Spor): RecordMetadata = kafkaProducer.send(record(spor)).get()

    private fun <V> record(value: V) = ProducerRecord<String, V>(sporingsloggTopic, value)
}

private val log = LoggerFactory.getLogger("SporingsloggKafkaClient")

/**
 * Definert her https://confluence.adeo.no/display/KES/Sporingslogg
 *
 * @param dataForespoersel Request-objektet
 */
data class Spor(
    val person: String,
    val mottaker: String,
    val tema: String,
    val behandlingsGrunnlag: String,
    val uthentingsTidspunkt: LocalDateTime,
    val leverteData: String,
    val samtykkeToken: String? = null,
    val dataForespoersel: String?,
    val leverandoer: String? = null,
) {
    companion object {
        fun opprett(
            personIdent: String,
            utlevertData: Any,
            requestObjekt: Any,
            konsumentOrgNr: String
        ): Spor {
            var MAX_SIZE = 1_050_000
            val reduction = 50_000
            val jsonStringified = objectMapper.writeValueAsString(utlevertData)

            var orginalMeldingTilSporing: Spor

            do {
                MAX_SIZE-=reduction
                orginalMeldingTilSporing = Spor(
                person = personIdent,
                mottaker = konsumentOrgNr,
                tema = "AAP",
                behandlingsGrunnlag = Consumers.getBehandlingsgrunnlag(konsumentOrgNr),
                uthentingsTidspunkt = LocalDateTime.now(),
                dataForespoersel = DefaultJsonMapper.toJson(requestObjekt),
                leverteData = Base64.getEncoder()
                    .encodeToString(
                        jsonStringified.take(minOf(jsonStringified.length, MAX_SIZE))
                            .encodeToByteArray()
                    ),
            )
            } while(objectMapper.writeValueAsString(orginalMeldingTilSporing).length > MAX_SPORINGSLOGG_SIZE)

            if (MAX_SIZE < 1_000_000) {
                log.warn("Leverte data er større enn 1MB, ${jsonStringified.length-MAX_SIZE} bytes med data er trunkert. Konsument: $konsumentOrgNr.")
            }

            return orginalMeldingTilSporing
        }

        private val objectMapper = jacksonObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(JavaTimeModule())
    }
}
