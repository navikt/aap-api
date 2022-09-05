import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import kafka.Topics
import kotlinx.coroutines.delay
import no.nav.aap.dto.kafka.IverksettVedtakKafkaDto
import org.apache.kafka.streams.TestInputTopic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class VedtakRouteTest {

    @Test
    fun `vedtak route accessible`() {
        Mocks().use { mocks ->
            lateinit var vedtakTopic: TestInputTopic<String, IverksettVedtakKafkaDto>

            testApplication {
                environment { config = mocks.environmentVariables }
                application {
                    api(mocks.kafka)
                    vedtakTopic = mocks.kafka.inputTopic(Topics.vedtak)
                }

                while (client.get("/actuator/ready").status != HttpStatusCode.OK) delay(10)

                val client = createClient {
                    install(ContentNegotiation) {
                        jackson {
                            registerModule(JavaTimeModule())
                            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        }
                    }
                }


                vedtakTopic.produce("1234") {
                    IverksettVedtakKafkaDto(
                        vedtaksid = UUID.fromString("ac9a1900-dbdc-4f89-b646-d8649a534e26"),
                        innvilget = false,
                        grunnlagsfaktor = 2.3,
                        vedtaksdato = LocalDate.now(),
                        virkningsdato = LocalDate.now(),
                        fødselsdato = LocalDate.now().minusYears(37)
                    )
                }

                val response = client.get("/vedtak/1234")
                assertEquals(HttpStatusCode.OK, response.status)

                val dao = response.body<IverksettVedtakKafkaDto>()
                assertEquals(UUID.fromString("ac9a1900-dbdc-4f89-b646-d8649a534e26"), dao.vedtaksid)
            }
        }
    }
}
