package api

import api.api_intern.ApiInternClient
import api.api_intern.IApiInternClient
import api.auth.MASKINPORTEN_AFP_OFFENTLIG
import api.auth.MASKINPORTEN_AFP_PRIVAT
import api.auth.MASKINPORTEN_TP_ORDNINGEN
import api.auth.maskinporten
import api.sporingslogg.KafkaFactory
import api.sporingslogg.Spor
import api.sporingslogg.SporingsloggKafkaClient
import api.tp.ITpRegisterClient
import api.tp.TpRegisterClient
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
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import org.apache.kafka.clients.producer.Producer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("App")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080) {
        val config = Config()
        api(
            config = Config(),
            kafkaProducer = KafkaFactory.createProducer(
                "aap-api-producer-${config.sporingslogg.topic}",
                config.kafka
            ),
            tpRegisterClient = TpRegisterClient
        )
    }.start(wait = true)
}

fun Application.api(
    config: Config,
    kafkaProducer: Producer<String, Spor>,
    apiInternClient: IApiInternClient = ApiInternClient(config.apiInternConfig),
    tpRegisterClient: ITpRegisterClient
) {
    val sporingsloggKafkaClient = SporingsloggKafkaClient(
        config.sporingslogg.topic,
        kafkaProducer
    )

    install(CallLogging) {
        callIdMdc("x-callid")
        logging()
    }

    install(CallId) {
        retrieveFromHeader("x-callid")
        verify { callId -> callId.isNotEmpty() }
    }

    install(MicrometerMetrics) {
        registry = prometheus
        meterBinders += LogbackMetrics()
    }

    install(StatusPages) {
        feilhåndtering(logger, prometheus)
    }

    install(ContentNegotiation) {
        jackson(ContentType.Application.Json.withCharset(Charsets.UTF_8)) {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    install(Authentication) {
        maskinporten(
            MASKINPORTEN_AFP_PRIVAT,
            listOf(config.oauth.maskinporten.scope.afpprivat),
            config
        )
        maskinporten(
            MASKINPORTEN_AFP_OFFENTLIG,
            listOf(
                config.oauth.maskinporten.scope.afpoffentlig,
                config.oauth.maskinporten.scope.afpoffentligAksio
            ),
            config
        )
        maskinporten(
            MASKINPORTEN_TP_ORDNINGEN,
            listOf(
                config.oauth.maskinporten.scope.tpordningen,
                config.oauth.maskinporten.scope.delegertTpOrdningen,
            ),
            config
        )
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        actuator(prometheus)
        swaggerUI(path = "swagger", swaggerFile = "openapi.yaml") {
            deepLinking = true
        }

        api(
            config.sporingslogg.enabled,
            apiInternClient,
            sporingsloggKafkaClient,
            tpRegisterClient,
            prometheus
        )
    }
}
