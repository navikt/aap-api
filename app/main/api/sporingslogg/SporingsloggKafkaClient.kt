package api.sporingslogg

import api.afp.VedtakRequestMedSaksRef
import api.util.Consumers
import api.util.KafkaConfig
import api.util.SporingsloggConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import java.time.LocalDateTime
import java.util.*

class SporingsloggException(cause: Throwable) : Exception(cause)

class SporingsloggKafkaClient(kafkaConf: KafkaConfig, private val sporingConf: SporingsloggConfig) {
    private val producer = KafkaFactory.createProducer<Spor>("aap-api-producer-${sporingConf.topic}", kafkaConf)

    fun send(spor: Spor): RecordMetadata = producer.send(record(spor)).get()

    private fun <V> record(value: V) = ProducerRecord<String, V>(sporingConf.topic, value)
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
