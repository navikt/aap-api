package api.sporingslogg

import api.util.SporingsloggConfig
import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.kafka.streams.v2.producer.ProducerConfig
import no.nav.aap.kafka.streams.v2.serde.JsonSerde
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord


class SporingsloggKafkaClient(streamsConfig: StreamsConfig, sporingsloggConfig: SporingsloggConfig) {
    private val sporingloggTopic = Topic(sporingsloggConfig.topic, JsonSerde.jackson<SporingsloggEntry>())
    private val producer: KafkaProducer<String, SporingsloggEntry>

    init {
        val properties = ProducerConfig(streamsConfig).toProperties(
            clientId = "aap-api-producer-${sporingsloggConfig.topic}"
        )
        producer = KafkaProducer(properties, sporingloggTopic.keySerde.serializer(), sporingloggTopic.valueSerde.serializer())
    }

    fun sendMelding(entry: SporingsloggEntry) {
        producer.send(ProducerRecord(sporingloggTopic.name,entry))
    }
}