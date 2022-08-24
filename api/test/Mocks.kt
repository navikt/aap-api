import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import søker.SøkereKafkaDto
import vedtak.VedtakKafkaDto
import java.time.LocalDate
import java.util.*

class Mocks : AutoCloseable {
    private val sink = embeddedServer(Netty, port = 0, module = Application::sinkMock).apply { start() }

    override fun close() {
        sink.stop(0, 0)
    }

    internal val environmentVariables = MapApplicationConfig(
        "KAFKA_STREAMS_APPLICATION_ID" to "api",
        "KAFKA_BROKERS" to "mock://kafka",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to "",
        "KAFKA_CLIENT_ID" to "api",
        "SINK_HOST" to "http://localhost:${sink.port}",
    )

    companion object {
        val NettyApplicationEngine.port get() = runBlocking { resolvedConnectors() }.first { it.type == ConnectorType.HTTP }.port
    }
}

private fun Application.sinkMock() {
    install(ContentNegotiation) { jackson() }
    routing {

        get("/vedtak/{personident}/latest") {
            val personident = call.parameters["personident"] ?: error("personident required")
            val vedtakDao = defaultDao(personident) {
                VedtakKafkaDto(
                    vedtaksid = UUID.randomUUID(),
                    innvilget = true,
                    grunnlagsfaktor = 2.3,
                    vedtaksdato = LocalDate.now(),
                    virkningsdato = LocalDate.now(),
                    fødselsdato = LocalDate.now().minusYears(37)
                )
            }

            call.respond(HttpStatusCode.OK, vedtakDao)
        }

        get("/soker/{personident}/latest") {
            val personident = call.parameters["personident"] ?: error("personident required")
            val søkerDao = defaultDao<SøkereKafkaDto>(personident) { null }

            call.respond(HttpStatusCode.OK, søkerDao)
        }
    }
}

internal val jackson = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private inline fun <reified T> defaultDao(
    personident: String,
    record: () -> T?,
): Dao = Dao(
    personident,
    jackson.writeValueAsString(record()) ?: "tombstone",
    null,
    0,
    0L,
    "dummy-topic",
    0L,
    0L,
    0L,
)
