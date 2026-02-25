package api

import api.afp.VedtakRequest
import api.util.Config
import api.util.UgyldigForespørselFeil
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.aap.api.intern.InternVedtakRequestApiIntern
import no.nav.aap.api.intern.Medium
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import api.sporingslogg.JacksonSerializer
import api.sporingslogg.Spor
import api.api_intern.IApiInternClient
import no.nav.aap.api.intern.Maksimum
import no.nav.aap.api.intern.PerioderResponse
import api.tp.ITpRegisterClient

internal class ErrorResponseTest {
    companion object {
        private val server = MockOAuth2Server()

        @BeforeAll
        @JvmStatic
        fun setup() {
            // Set required properties (copied from AfpOffentligServerTest)
            System.setProperty("AAP_AUDIENCE", "http://kafka")
            System.setProperty("ALTINN_WELLKNOWN", "http://kafka")
            System.setProperty("ALTINN_AUDIENCE", "http://kafka")
            System.setProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "http://kafka")
            System.setProperty("AZURE_APP_CLIENT_ID", "http://kafka")
            System.setProperty("AZURE_APP_CLIENT_SECRET", "http://kafka")
            System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "http://kafka")
            System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "http://kafka")
            System.setProperty("KAFKA_BROKERS", "http://kafka")
            System.setProperty("KAFKA_TRUSTSTORE_PATH", "http://kafka")
            System.setProperty("KAFKA_KEYSTORE_PATH", "http://kafka")
            System.setProperty("KAFKA_CREDSTORE_PASSWORD", "http://kafka")
            System.setProperty("SPORINGSLOGG_ENABLED", "true")
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
    fun `Test error response consistency`() = testApplication {
        application {
            api(
                Config(),
                MockProducer(true, null, StringSerializer(), JacksonSerializer<Spor>()),
                mockApiInternClient(),
                mockTpRegisterClient()
            )
        }

        val client = createClient {
            install(ContentNegotiation) {
                jackson()
            }
        }
        val jwt = server.issueToken(issuerId = "default", claims = mapOf("scope" to "nav:aap:afpprivat.read", "consumer" to mapOf("authority" to "123", "ID" to "0192:938708606")))

        // Test 1: Invalid JSON (ContentTransformationException) -> Currently returns String
        val responseJsonError = client.post("/afp/fellesordningen") {
            header("Authorization", "Bearer ${jwt.serialize()}")
            header("X-callid", UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
            setBody("{ invalid json }")
        }
        
        assertEquals(HttpStatusCode.BadRequest, responseJsonError.status)
        val body1 = responseJsonError.body<UgyldigForespørselFeil>()
        println("Response 1 (Invalid JSON): $body1")
        assertTrue(body1.melding == "Feil i mottatte data", "Expected 'Feil i mottatte data' but got '${body1.melding}'")
        assertEquals("FEIL_I_DATA", body1.kode)

        // Test 2: Invalid Date Range (PeriodeErrorException) -> Currently returns String
        // We need a valid JSON but with logical error to trigger PeriodeErrorException
        // Looking at VedtakRequest init block
        val responseDateError = client.post("/afp/fellesordningen") {
            header("Authorization", "Bearer ${jwt.serialize()}")
            header("X-callid", UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
            setBody("""{"personidentifikator":"123","fraOgMedDato":"2025-01-01","tilOgMedDato":"2024-01-01"}""") // From > To
        }

        assertEquals(HttpStatusCode.BadRequest, responseDateError.status)
        val body2 = responseDateError.body<UgyldigForespørselFeil>()
        println("Response 2 (Invalid Date): $body2")
        assertTrue(body2.melding.contains("må være mindre eller lik"))
        assertEquals("FEIL_I_PERIODE", body2.kode)
    }
    
    private fun mockApiInternClient() = object : IApiInternClient {
        override fun hentMaksimum(callId: String, vedtakRequest: InternVedtakRequestApiIntern) = Maksimum(listOf())
        override fun hentPerioder(callId: UUID, vedtakRequest: VedtakRequest) = PerioderResponse(listOf())
        override fun hentMedium(vedtakRequest: InternVedtakRequestApiIntern) = Medium(listOf())
    }

    private fun mockTpRegisterClient() = object : ITpRegisterClient {
        override fun brukerHarTpForholdOgYtelse(fnr: String, orgnr: String, requestId: String) = true
    }
}
