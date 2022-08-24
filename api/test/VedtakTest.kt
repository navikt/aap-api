import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import vedtak.VedtakKafkaDto

internal class VedtakRouteTest {

    @Test
    fun `vedtak route accessible`() {
        Mocks().use { mocks ->
            testApplication {
                environment { config = mocks.environmentVariables }
                application { api() }

                val client = createClient {
                    install(ContentNegotiation) {
                        jackson {
                            registerModule(JavaTimeModule())
                            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        }
                    }
                }

                val response = client.get("/vedtak/1234")
                assertEquals(HttpStatusCode.OK, response.status)

                val dao = response.body<VedtakKafkaDto>()
                assertEquals(2.3, dao.grunnlagsfaktor)
            }
        }
    }
}
