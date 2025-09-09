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
            val jsonStringified = objectMapper.writeValueAsString(utlevertData)
            val maksEnMillTegn = jsonStringified.take(minOf(jsonStringified.length, 1_000_000))

            if (jsonStringified.length > 1_000_000) {
                log.warn("Leverte data er større enn 1MB, data er trunkert. Konsument: $konsumentOrgNr.")
            }

            return Spor(
                person = personIdent,
                mottaker = konsumentOrgNr,
                tema = "AAP",
                behandlingsGrunnlag = Consumers.getBehandlingsgrunnlag(konsumentOrgNr),
                uthentingsTidspunkt = LocalDateTime.now(),
                dataForespoersel = DefaultJsonMapper.toJson(requestObjekt),
                leverteData = Base64.getEncoder()
                    .encodeToString(maksEnMillTegn.encodeToByteArray()),
            )
        }

        private val objectMapper = jacksonObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(JavaTimeModule())
    }
}
