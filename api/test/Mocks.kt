import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.kafka.streams.v2.test.KStreamsMock

class Mocks : AutoCloseable {

    private val oauth = embeddedServer(Netty, port = 9999) {
        install(ContentNegotiation) { jackson {} }
        routing {
            get("/jwks") {
                call.respondText(this::class.java.getResource("/jwkset.json")!!.readText())
            }
        }
    }.start()

    val kafka = KStreamsMock()

    override fun close() {
        oauth.stop()
    }

    internal val environmentVariables = MapApplicationConfig(
        "KAFKA_STREAMS_APPLICATION_ID" to "api",
        "KAFKA_BROKERS" to "mock://kafka",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to "",
        "KAFKA_CLIENT_ID" to "api",
        "MASKINPORTEN_ISSUER" to "maskinporten",
        "MASKINPORTEN_WELL_KNOWN_URL" to "",
        "AAP_AUDIENCE" to "aap",
        "MASKINPORTEN_JWKS_URI" to "http://0.0.0.0:9999/jwks"
    )
}

