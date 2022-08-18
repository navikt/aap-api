package routing

import api
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import no.nav.aap.kafka.streams.test.KafkaStreamsMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ActuatorsRouteTest {

    @Test
    fun `liveness configured`() {
//        KafkaStreamsMock().use { kafka ->
            testApplication {
                environment { config = environmentVariables }
                application { api() }

                val liveness = client.get("/actuator/live")
                assertEquals(HttpStatusCode.OK, liveness.status)
            }
//        }
    }

    @Test
    fun `readiness configured`() {
//        KafkaStreamsMock().use { kafka ->
            testApplication {
                environment { config = environmentVariables }
                application { api() }

                val readiness = client.get("/actuator/ready")
                assertEquals(HttpStatusCode.OK, readiness.status)
            }
//        }
    }

    private val environmentVariables = MapApplicationConfig(
        "KAFKA_STREAMS_APPLICATION_ID" to "api",
        "KAFKA_BROKERS" to "mock://kafka",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to "",
        "KAFKA_CLIENT_ID" to "api",
    )
}
