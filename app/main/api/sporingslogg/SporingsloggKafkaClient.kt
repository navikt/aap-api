package api.sporingslogg

import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.kafka.streams.v2.producer.ProducerConfig
import no.nav.aap.kafka.streams.v2.serde.JsonSerde
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

private const val SPORINGSLOGG_TOPIC = "aapen-sporingslogg-loggmeldingMottatt"

class SporingsloggKafkaClient(streamsConfig: StreamsConfig) {
    private val sporingloggTopic = Topic(SPORINGSLOGG_TOPIC, JsonSerde.jackson<SporingsloggEntry>())
    private val producer: KafkaProducer<String, SporingsloggEntry>

    init {
        val properties = ProducerConfig(streamsConfig).toProperties(
            clientId = "aap-api-producer-$SPORINGSLOGG_TOPIC"
        )
        producer = KafkaProducer(properties, sporingloggTopic.keySerde.serializer(), sporingloggTopic.valueSerde.serializer())
    }

    fun sendMelding(entry: SporingsloggEntry) {
        producer.send(ProducerRecord(sporingloggTopic.name,entry))
    }
}