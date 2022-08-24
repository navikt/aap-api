import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import infrastructure.HttpClientFactory
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.ktor.config.loadConfig
import routing.actuators
import routing.meldepliktshendelser
import søker.søker
import vedtak.vedtak

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api() {
    val config = loadConfig<Config>("/config.yml")
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) { registry = prometheus }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    val sinkClient = HttpClientFactory.create()

    routing {
        vedtak(config, sinkClient)
        søker(config, sinkClient)
        meldepliktshendelser()
        actuators(prometheus)
    }
}

data class Dao(
    val personident: String,
    val record: String,
    val dtoVersion: Int?,
    val partition: Int,
    val offset: Long,
    val topic: String,
    val timestamp: Long,
    val systemTimeMs: Long,
    val streamTimeMs: Long,
)

internal class HttpClientLogger(private val log: org.slf4j.Logger) : Logger {
    override fun log(message: String) = log.info(message)
}
