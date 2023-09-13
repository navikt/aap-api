package api

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import api.kafka.Tables
import no.nav.aap.kafka.streams.v2.Streams
import no.nav.aap.kafka.streams.v2.KafkaStreams
import no.nav.aap.kafka.streams.v2.Topology
import no.nav.aap.kafka.streams.v2.processor.state.GaugeStoreEntriesStateScheduleProcessor
import no.nav.aap.kafka.streams.v2.topology
import no.nav.aap.ktor.config.loadConfig
import org.slf4j.LoggerFactory
import api.routes.actuatorRoutes
import api.routes.vedtak
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.dsl.AuthScheme
import io.github.smiley4.ktorswaggerui.dsl.AuthType
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val logger = LoggerFactory.getLogger("App")

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api(kafka: Streams = KafkaStreams()) {
    val config = loadConfig<Config>()
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) { registry = prometheus }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    install(SwaggerUI) {
        swagger {
            swaggerUrl = "swagger-ui"
            forwardRoot = true
        }
        info {
            title = "AAP - Api"
            version = "latest"
            description = ""
        }
        server {
            url = "http://localhost:8080"
            description = ""
        }
        securityScheme("Maskinporten") {
            type = AuthType.HTTP
            scheme = AuthScheme.BEARER
        }
    }

    val jwkProvider: JwkProvider = JwkProviderBuilder(config.oauth.maskinporten.jwksUri)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt {
            verifier(jwkProvider, config.oauth.maskinporten.issuer.name)
            challenge { _, _ -> call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang") }
            validate { cred ->
                if (!cred.audience.contains(config.oauth.maskinporten.issuer.audience)) {
                    logger.warn("Audience does not match")
                    return@validate null
                }
                if (cred.getClaim("scopes", String::class) != config.oauth.maskinporten.scope.vedtak) {
                    logger.warn("Wrong scope in claim")
                    return@validate null
                }

                JWTPrincipal(cred.payload)
            }
        }
    }

    Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
    environment.monitor.subscribe(ApplicationStopping) { kafka.close() }

    kafka.connect(
        config = config.kafka,
        registry = prometheus,
        topology = topology(prometheus)
    )

    val vedtakStore = kafka.getStore(Tables.vedtak)

    routing {
        actuatorRoutes(prometheus, kafka)
        vedtak(vedtakStore)
    }
}

internal fun topology(prometheus: MeterRegistry): Topology = topology{

    val vedtakTable = consume(Tables.vedtak)

    vedtakTable.schedule(GaugeStoreEntriesStateScheduleProcessor(
        ktable = vedtakTable,
        interval = 2.toDuration(DurationUnit.MINUTES),
        registry = prometheus
    ))
}
