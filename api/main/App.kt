import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.kafka.streams.KStreams
import no.nav.aap.kafka.streams.KafkaStreams
import no.nav.aap.ktor.config.loadConfig
import org.apache.kafka.streams.StreamsBuilder
import routing.actuators

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api(kafka: KStreams = KafkaStreams) {
    val config = loadConfig<Config>("/config.yml")
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) { registry = prometheus }

    kafka.connect(
        config = config.kafka,
        registry = prometheus,
        topology = StreamsBuilder().build(),
    )

    routing {
        actuators(prometheus, kafka)
    }
}
