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
import api.util.Config
import api.util.actuator
import api.util.feilhåndtering
import api.util.logging
import api.util.prometheus
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.withCharset
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
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
            listOf(config.oauth.maskinporten.scope.tpordningen),
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
