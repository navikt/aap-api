import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import kafka.Topics
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.aap.dto.kafka.IverksettVedtakKafkaDto
import no.nav.aap.kafka.streams.test.TestTopic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class VedtakRouteTest {

    @Test
    fun `vedtak route accessible`() {
        Mocks().use { mocks ->
            lateinit var vedtakTopic: TestTopic<IverksettVedtakKafkaDto>

            testApplication {
                environment { config = mocks.environmentVariables }
                application {
                    api(mocks.kafka)
                    vedtakTopic = mocks.kafka.testTopic(Topics.vedtak)
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

                assertEquals(UUID.fromString("ac9a1900-dbdc-4f89-b646-d8649a534e26"), client.getVedtak("/vedtak/1234", JwtGenerator::generateToken).vedtaksid)
            }
        }
    }

    private fun HttpClient.getVedtak(path: String, tokenSupplier: () -> SignedJWT): IverksettVedtakKafkaDto = runBlocking {
        val response = get(path) {
            bearerAuth(tokenSupplier().serialize())
            accept(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        response.body()
    }
}
