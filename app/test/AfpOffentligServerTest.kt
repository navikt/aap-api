import api.afp.VedtakRequest
import api.afp.VedtakRequestMedSaksRef
import api.api
import api.api_intern.IApiInternClient
import api.arena.IArenaoppslagRestClient
import api.dsop.DsopRequest
import api.sporingslogg.Spor
import api.tp.ITpRegisterClient
import api.util.Config
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.LocalDate
import java.util.UUID
import no.nav.aap.api.intern.Medium
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.ekstern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.MockProducer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class AfpOffentligServerTest {
    companion object {
        private val server = MockOAuth2Server()

        @BeforeAll
        @JvmStatic
        fun setup() {
            System.setProperty("AAP_AUDIENCE", "http://kafka")
            System.setProperty("ALTINN_WELLKNOWN", "http://kafka")
            System.setProperty("ALTINN_AUDIENCE", "http://kafka")
            System.setProperty("ARENAOPPSLAG_PROXY_BASE_URL", "http://kafka")
            System.setProperty("ARENAOPPSLAG_SCOPE", "http://kafka")
            System.setProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "http://kafka")
            System.setProperty("AZURE_APP_CLIENT_ID", "http://kafka")
            System.setProperty("AZURE_APP_CLIENT_SECRET", "http://kafka")
            System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "http://kafka")
            System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "http://kafka")
            System.setProperty("KAFKA_BROKERS", "http://kafka")
            System.setProperty("KAFKA_TRUSTSTORE_PATH", "http://kafka")
            System.setProperty("KAFKA_KEYSTORE_PATH", "http://kafka")
            System.setProperty("KAFKA_CREDSTORE_PASSWORD", "http://kafka")
            System.setProperty("SPORINGSLOGG_ENABLED", "http://kafka")
            System.setProperty("SPORINGSLOGG_TOPIC", "http://kafka")
            System.setProperty("API_INTERN_URL", "http://kafka")
            System.setProperty("API_INTERN_SCOPE", "http://kafka")

            server.start()

            val wellnowurl = server.wellKnownUrl("default").toString()
            val jwksuri = server.jwksUrl("default").toString()

            System.setProperty("MASKINPORTEN_JWKS_URI", jwksuri)
            System.setProperty("MASKINPORTEN_ISSUER", server.issuerUrl("default").toString())
            System.setProperty("MASKINPORTEN_WELL_KNOWN_URL", wellnowurl)
            System.setProperty("AAP_AUDIENCE", "default")
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            server.shutdown()
        }
    }

    @Test
    fun testAfpOffentlig() = testApplication {
        val mockProducer = MockProducer<String, Spor>()
        val apiInternClient = ApiInternKlient()

        application {
            api(
                Config(), mockProducer,
                apiInternClient,
                tpRegisterKlient()
            )
        }
        val client = createClient()
        val jwt = issueToken("nav:aap:afpoffentlig.read")

        val response = sendPostRequest(
            client, jwt, VedtakRequestMedSaksRef(
                personidentifikator = "123",
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now(),
                saksId = "dasdasd"
            ), "/afp/offentlig"
        )
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            api.afp.VedtakResponse(perioder = listOf()),
            response.body() as api.afp.VedtakResponse
        )
    }

    @Test
    fun `hent ut dummy-vedtak fra tp-ordningen`() = testApplication {
        val mockProducer = MockProducer<String, Spor>()
        val apiInternClient = ApiInternKlient()

        application {
            api(
                Config(), mockProducer,
                apiInternClient,
                tpRegisterKlient(),
            )
        }
        val client = createClient()
        val jwt = issueToken("nav:aap:tpordningen.read")

        val response = sendPostRequest(
            client, jwt, VedtakRequest(
                personidentifikator = "123",
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now(),
            ), "/tp-samhandling"
        )
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            api.Maksimum(vedtak = listOf()),
            response.body() as api.Maksimum
        )
    }

    @Test
    fun `får 404 ved negativt svar fra tp-ordningen`() = testApplication {
        val mockProducer = MockProducer<String, Spor>()
        val apiInternClient = ApiInternKlient()

        application {
            api(
                Config(), mockProducer,
                apiInternClient,
                // Får false fra TP-registeret
                tpRegisterKlient(false),
            )
        }
        val client = createClient()
        val jwt = issueToken("nav:aap:tpordningen.read")

        val response = sendPostRequest(
            client, jwt, VedtakRequest(
                personidentifikator = "123",
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now(),
            ), "/tp-samhandling"
        )
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(
            "Mangler TP-ytelse.",
            response.bodyAsText()
        )
    }

    @Test
    fun `AFP felles - Ugyldig request gir 400 Bad request`() = testApplication {
        application {
            api(
                Config(),
                MockProducer(),
                ApiInternKlient(),
                tpRegisterKlient(),
            )
        }

        val jwt = issueToken("nav:aap:afpprivat.read")

        val response = client.post("/afp/fellesordningen") {
            header("Authorization", "Bearer ${jwt.serialize()}")
            header("X-callid", UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
            setBody("""{"personidentifikator":"1234","fraOgMedDato":"2025-01-01","tilOgMedDato":"2024-01-01","saksId":"123"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `AFP offentlig - Ugyldig request gir 400 Bad request`() = testApplication {
        application {
            api(
                Config(),
                MockProducer(),
                ApiInternKlient(),
                tpRegisterKlient(),
            )
        }

        val jwt = issueToken("nav:aap:afpoffentlig.read")

        val response = client.post("/afp/offentlig") {
            header("Authorization", "Bearer ${jwt.serialize()}")
            header("X-callid", UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
            setBody("""{"personidentifikator":"1234","fraOgMedDato":"2025-01-01","tilOgMedDato":"2024-01-01","saksId":"123"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @ParameterizedTest
    @CsvSource(
        "/tp-samhandling",
        "/tp-samhandling-med-utbetalinger",
    )
    fun `TP Samhandling - Ugyldig request gir 400 Bad request`(endepunkt: String) = testApplication {
        application {
            api(
                Config(),
                MockProducer(),
                ApiInternKlient(),
                tpRegisterKlient(),
            )
        }

        val jwt = issueToken("nav:aap:tpordningen.read")

        val response = client.post(endepunkt) {
            header("Authorization", "Bearer ${jwt.serialize()}")
            header("X-callid", UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
            setBody("""{"personidentifikator":"1234","fraOgMedDato":"2025-01-01","tilOgMedDato":"2024-01-01"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    private fun tpRegisterKlient(ønsketSvar: Boolean? = true) = object : ITpRegisterClient {
        override fun brukerHarTpForholdOgYtelse(
            fnr: String,
            orgnr: String,
            requestId: String
        ): Boolean? {
            return ønsketSvar
        }

    }

    private fun ApiInternKlient() = object : IApiInternClient {
        override fun hentMaksimum(
            callId: String,
            vedtakRequest: EksternVedtakRequest
        ): no.nav.aap.api.intern.Maksimum {
            return no.nav.aap.api.intern.Maksimum(vedtak = listOf())
        }

        override fun hentPerioder(
            callId: UUID,
            vedtakRequest: VedtakRequest
        ): no.nav.aap.api.intern.PerioderResponse {
            return no.nav.aap.api.intern.PerioderResponse(perioder = listOf())
        }

        override fun hentMedium(vedtakRequest: EksternVedtakRequest): Medium {
            return Medium(vedtak = listOf())
        }
    }

    private fun arenaOppslagKlient() = object : IArenaoppslagRestClient {
        override suspend fun hentMaksimum(
            callId: String,
            vedtakRequest: EksternVedtakRequest
        ): Maksimum {
            return Maksimum(
                vedtak = listOf()
            )
        }

        override suspend fun hentVedtakFellesordning(
            callId: UUID,
            vedtakRequest: VedtakRequest
        ): VedtakResponse {
            return VedtakResponse(
                perioder = listOf()
            )
        }

        override suspend fun hentVedtakDsop(
            callId: UUID,
            dsopRequest: DsopRequest
        ): no.nav.aap.arenaoppslag.kontrakt.dsop.VedtakResponse {
            TODO("Not yet implemented")
        }

        override suspend fun hentMeldepliktDsop(
            callId: UUID,
            dsopRequest: DsopRequest
        ): no.nav.aap.arenaoppslag.kontrakt.dsop.VedtakResponse {
            TODO("Not yet implemented")
        }
    }

    private fun ApplicationTestBuilder.createClient() = createClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
    }

    private suspend fun sendPostRequest(
        client: HttpClient,
        jwt: SignedJWT,
        payload: Any,
        path: String
    ) = client.post(path) {
        header("Authorization", "Bearer ${jwt.serialize()}")
        header("X-callid", UUID.randomUUID().toString())
        contentType(ContentType.Application.Json)
        setBody(payload)
    }

    private fun issueToken(scope: String) = server.issueToken(
        issuerId = "default",
        claims = mapOf(
            "scope" to scope,
            "consumer" to mapOf("authority" to "123", "ID" to "0192:889640782")
        ),
    )
}