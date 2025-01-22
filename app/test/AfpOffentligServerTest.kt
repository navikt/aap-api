import api.afp.VedtakRequest
import api.afp.VedtakRequestMedSaksRef
import api.api
import api.arena.IArenaoppslagRestClient
import api.dsop.DsopRequest
import api.sporingslogg.Spor
import api.tp.ITpRegisterClient
import api.tp.TpRegisterClient
import api.util.Config
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
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.ekstern.VedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.MockProducer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class AfpOffentligServerTest {
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
        val arenaRestClient = arenaOppslagKlient()

        application {
            api(
                Config(), mockProducer,
                arenaRestClient,
                tpRegisterKlient(),
            )
        }
        val client = createClient()
        val jwt = issueToken("nav:aap:afpoffentlig.read")

        val response = sendPostRequest(
            client, jwt, VedtakRequestMedSaksRef(
                personidentifikator = "123",
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now(),
                saksId = null
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
        val arenaRestClient = arenaOppslagKlient()

        application {
            api(
                Config(), mockProducer,
                arenaRestClient,
                tpRegisterKlient(),
            )
        }
        val client = createClient()
        val jwt = issueToken("nav:aap:tpordningen.read")

        val response = sendPostRequest(
            client, jwt, VedtakRequestMedSaksRef(
                personidentifikator = "123",
                fraOgMedDato = LocalDate.now(),
                tilOgMedDato = LocalDate.now(),
                saksId = null
            ), "/tp-samhandling"
        )
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            api.Maksimum(vedtak = listOf()),
            response.body() as api.Maksimum
        )
    }

    private fun tpRegisterKlient() = object : ITpRegisterClient {
        override fun brukerHarTpForholdOgYtelse(
            fnr: String,
            orgnr: String,
            requestId: String
        ): Boolean {
            return true
        }

    }

    private fun arenaOppslagKlient() = object : IArenaoppslagRestClient {
        override fun hentMaksimum(
            callId: String,
            vedtakRequest: EksternVedtakRequest
        ): Maksimum {
            return Maksimum(
                vedtak = listOf()
            )
        }

        override fun hentVedtakFellesordning(
            callId: UUID,
            vedtakRequest: VedtakRequest
        ): VedtakResponse {
            return VedtakResponse(
                perioder = listOf()
            )
        }

        override fun hentVedtakDsop(
            callId: UUID,
            dsopRequest: DsopRequest
        ): no.nav.aap.arenaoppslag.kontrakt.dsop.VedtakResponse {
            TODO("Not yet implemented")
        }

        override fun hentMeldepliktDsop(
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
        payload: VedtakRequestMedSaksRef,
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
            "consumer" to mapOf("authority" to "123")
        ),
    )
}