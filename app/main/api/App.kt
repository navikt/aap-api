package api

import api.arena.ArenaoppslagRestClient
import api.auth.MASKINPORTEN_FELLESORDNING
import api.auth.maskinporten
import api.fellesordningen.fellesordningen
import api.sporingslogg.SporingsloggKafkaClient
import api.util.Config
import api.util.actuator
import api.util.feilhåndtering
import api.util.logging
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.ktor.config.loadConfig
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("App")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api() {
    val config = loadConfig<Config>()
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val sporingsloggKafkaClient = SporingsloggKafkaClient(config.kafka)

    install(CallLogging) {
        logging()
    }

    install(MicrometerMetrics) {
        registry = prometheus
    }

    install(StatusPages) {
        feilhåndtering(logger)
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    install(Authentication) {
        maskinporten(MASKINPORTEN_FELLESORDNING, config.oauth.maskinporten.scope.afpprivat, config)
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val arenaRestClient = ArenaoppslagRestClient(config.arenaoppslag, config.azure)

    routing {
        actuator(prometheus)
        swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")
        authenticate(MASKINPORTEN_FELLESORDNING) {
            fellesordningen(arenaRestClient, sporingsloggKafkaClient)
        }
    }
}
