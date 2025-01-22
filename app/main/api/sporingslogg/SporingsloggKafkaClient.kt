package api.sporingslogg

import api.util.Consumers
import api.util.SporingsloggConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.producer.KafkaProducer
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

//https://confluence.adeo.no/display/KES/Sporingslogg
data class Spor(
    val person: String,
    val mottaker: String,
    val tema: String,
    val behandlingsGrunnlag: String,
    val uthentingsTidspunkt: LocalDateTime,
    val leverteData: String,
    val samtykkeToken: String? = null,
    val dataForespoersel: String? = null,
    val leverandoer: String? = null,
    val saksId: String? = null
) {
    companion object {
        fun opprett(
            personIdent: String,
            utlevertData: Any,
            konsumentOrgNr: String,
            saksId: String? = null
        ) = Spor(
            person = personIdent,
            mottaker = konsumentOrgNr,
            tema = "AAP",
            behandlingsGrunnlag = Consumers.getBehandlingsgrunnlag(konsumentOrgNr),
            uthentingsTidspunkt = LocalDateTime.now(),
            leverteData = Base64.getEncoder()
                .encodeToString(objectMapper.writeValueAsString(utlevertData).encodeToByteArray()),
            saksId = saksId
        )

        private val objectMapper = jacksonObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(JavaTimeModule())
    }
}
