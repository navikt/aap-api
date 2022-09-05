import io.ktor.server.config.*
import no.nav.aap.kafka.streams.test.KafkaStreamsMock
import org.apache.kafka.streams.TestInputTopic

class Mocks : AutoCloseable {
    val kafka = KafkaStreamsMock()

    override fun close() {}

    internal val environmentVariables = MapApplicationConfig(
        "KAFKA_STREAMS_APPLICATION_ID" to "api",
        "KAFKA_BROKERS" to "mock://kafka",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to "",
        "KAFKA_CLIENT_ID" to "api",
    )
}

inline fun <reified V : Any> TestInputTopic<String, V>.produce(key: String, value: () -> V) = pipeInput(key, value())
