import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.ktor.config.loadConfig
import org.slf4j.LoggerFactory
import routing.actuators
import routing.meldepliktshendelser
import routing.vedtak

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api() {
    val config = loadConfig<Config>("/config.yml")
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) { registry = prometheus }

//    kafka.connect(
//        config = config.kafka,
//        registry = prometheus,
//        topology = StreamsBuilder().build(),
//    )

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson { registerModule(JavaTimeModule()) } }
        install(HttpRequestRetry)
        install(HttpTimeout)
        install(Logging) {
            level = LogLevel.BODY
            logger = HttpClientLogger(LoggerFactory.getLogger("secureLog"))
        }
    }

    routing {
        vedtak(httpClient)
        meldepliktshendelser()
        actuators(prometheus)
    }
}

internal class HttpClientLogger(private val log: org.slf4j.Logger) : Logger {
    override fun log(message: String) = log.info(message)
}
