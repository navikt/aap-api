package api.sporingslogg

import api.util.KafkaConfig
import api.util.KafkaFactory
import api.util.SporingsloggConfig
import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.serde.JsonSerde
import org.apache.kafka.clients.producer.ProducerRecord


class SporingsloggKafkaClient(kafkaProducerConfig: KafkaConfig, sporingsloggConfig: SporingsloggConfig) {
    private val sporingloggTopic = Topic(sporingsloggConfig.topic, JsonSerde.jackson<SporingsloggEntry>())
    private val producer = KafkaFactory.createProducer<SporingsloggEntry>("aap-api-producer-${sporingsloggConfig.topic}", kafkaProducerConfig)

    fun sendMelding(entry: SporingsloggEntry) {
        producer.send(ProducerRecord(sporingloggTopic.name,entry))
    }
}