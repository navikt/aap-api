package api.sporingslogg

import api.util.Consumers
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
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
        ) = Spor(
            person = personIdent,
            mottaker = konsumentOrgNr,
            tema = "AAP",
            behandlingsGrunnlag = Consumers.getBehandlingsgrunnlag(konsumentOrgNr),
            uthentingsTidspunkt = LocalDateTime.now(),
            dataForespoersel = DefaultJsonMapper.toJson(requestObjekt),
            leverteData = Base64.getEncoder()
                .encodeToString(objectMapper.writeValueAsString(utlevertData).encodeToByteArray()),
        )

        private val objectMapper = jacksonObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(JavaTimeModule())
    }
}
