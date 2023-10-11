package api

import api.arena.ArenaoppslagRestClient
import api.auth.SamtykkeIkkeGittException
import api.auth.maskinporten
import api.fellesordningen.fellesordningen
import api.openapi.openAPIAuthenticatedRoute
import api.openapi.openApiJwtProvider
import api.sporingslogg.SporingsloggKafkaClient
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.ktor.config.loadConfig
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

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
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val callId = call.request.header("x-callId") ?: call.request.header("nav-callId") ?: "ukjent"
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent, callId: $callId"
        }
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }

    install(MicrometerMetrics) { registry = prometheus }

    install(StatusPages) {
        exception<SamtykkeIkkeGittException>{call, cause ->
            logger.warn("Samtykke ikke gitt", cause)
            call.respondText(text = "Samtykke ikke gitt", status = HttpStatusCode.Forbidden)
        }
        exception<Throwable> { call, cause ->
            logger.error("Uhåndtert feil", cause)
            call.respondText(text = "Feil i tjeneste: ${cause.message}" , status = HttpStatusCode.InternalServerError)
        }

    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    install(OpenAPIGen) {
        serveOpenApiJson = true
        serveSwaggerUi = true
        info {
            version = "1.0.0"
            title = "AAP - API"
            description = "API for å dele AAP-data med eksterne konsumenter"
        }
        addModules(openApiJwtProvider)
    }

    install(Authentication) {
        maskinporten(config)
    }

    val arenaRestClient = ArenaoppslagRestClient(config.arenaoppslag, config.azure)

    apiRouting {
        openAPIAuthenticatedRoute {
            fellesordningen(arenaRestClient, config, sporingsloggKafkaClient)
        }
        routing {
            route("/actuator") {
                get("/metrics") {
                    call.respondText(prometheus.scrape())
                }

                get("/live") {
                    call.respond(HttpStatusCode.OK, "api")
                }
                get("/ready") {
                    call.respond(HttpStatusCode.OK, "api")
                }
            }
        }
    }
}