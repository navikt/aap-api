package routing

import api
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtakRouteTest {

    @Test
    fun `vedtak route accessible`() {
        Mocks().use { mocks ->
            testApplication {
                environment { config = mocks.environmentVariables }
                application { api() }

                val response = client.get("/vedtak/1234")
                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }

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
}

private fun Application.sinkMock() {
    install(ContentNegotiation) { jackson() }
    routing {
        get("/soker/1234/latest") {
            val søkerDao = SøkerDao("1234", "", null, 0, 0L, "", 0L, 0L, 0L)
            call.respond(HttpStatusCode.OK, søkerDao)
        }
    }
}