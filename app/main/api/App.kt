package api

import api.arena.ArenaoppslagRestClient
import api.auth.MASKINPORTEN_AFP_OFFENTLIG
import api.auth.MASKINPORTEN_AFP_PRIVAT
import api.auth.MASKINPORTEN_TP_ORDNINGEN
import api.auth.maskinporten
import api.sporingslogg.SporingsloggKafkaClient
import api.util.*
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.swagger.codegen.v3.generators.html.StaticHtmlCodegen
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("App")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api() {
    val config = Config()
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val sporingsloggKafkaClient = SporingsloggKafkaClient(config.kafka, config.sporingslogg)
    val arenaRestClient = ArenaoppslagRestClient(config.arenaoppslag, config.azure)

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
        maskinporten(MASKINPORTEN_AFP_PRIVAT, config.oauth.maskinporten.scope.afpprivat, config)
        maskinporten(MASKINPORTEN_AFP_OFFENTLIG, config.oauth.maskinporten.scope.afpoffentlig, config)
        maskinporten(MASKINPORTEN_TP_ORDNINGEN,config.oauth.maskinporten.scope.tpordningen, config)
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        actuator(prometheus)
        swaggerUI(path = "swagger", swaggerFile = "openapi.bak.yaml")
        openAPI(path="openapi", swaggerFile = "openapi/documentation.yaml") {
            codegen = StaticHtmlCodegen()
        }
        api(config.sporingslogg.enabled, arenaRestClient, sporingsloggKafkaClient, prometheus)
    }
}
