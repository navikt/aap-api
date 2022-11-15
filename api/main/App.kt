import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.webjars.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kafka.Tables
import kafka.Topics
import no.nav.aap.dto.kafka.IverksettVedtakKafkaDto
import no.nav.aap.kafka.streams.KStreams
import no.nav.aap.kafka.streams.KafkaStreams
import no.nav.aap.kafka.streams.extension.consume
import no.nav.aap.kafka.streams.extension.produce
import no.nav.aap.kafka.streams.store.scheduleMetrics
import no.nav.aap.ktor.config.loadConfig
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import routes.actuatorRoutes
import routes.swaggerRoutes
import routes.vedtak
import java.time.LocalDate
import java.util.*
import kotlin.time.Duration.Companion.minutes

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.test() {
    install(Webjars) {
        path = "webjars"
    }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    routing {
        static("static") {
            resources("web")
        }

        get("/") {
            call.respondRedirect("/webjars/swagger-ui/index.html")
        }

        get("/vedtak/{personident}") {
            call.parameters.getOrFail("personident")

            call.respond(
                HttpStatusCode.OK, IverksettVedtakKafkaDto(
                    UUID.randomUUID(),
                    true,
                    2.3,
                    LocalDate.now(),
                    LocalDate.now(),
                    LocalDate.now()
                )
            )
        }
    }
}

fun Application.api(kafka: KStreams = KafkaStreams) {
    val config = loadConfig<Config>("/config.yml")
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(Webjars)
    install(MicrometerMetrics) { registry = prometheus }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
    environment.monitor.subscribe(ApplicationStopping) { kafka.close() }

    kafka.connect(
        config = config.kafka,
        registry = prometheus,
        topology = topology(prometheus)
    )

    val vedtakStore = kafka.getStore<IverksettVedtakKafkaDto>(Tables.vedtak.stateStoreName)

    routing {
        actuatorRoutes(prometheus, kafka)
        swaggerRoutes()
        vedtak(vedtakStore)
    }
}

internal fun topology(registry: MeterRegistry): Topology {
    val stream = StreamsBuilder()

    val vedtakTable = stream
        .consume(Topics.vedtak)
        .produce(Tables.vedtak)

    vedtakTable.scheduleMetrics(Tables.vedtak, 2.minutes, registry)

    return stream.build()
}
