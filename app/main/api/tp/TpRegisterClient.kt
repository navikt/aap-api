package api.tp

import java.net.URI
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.ContentType
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("api.tp.TpRegisterClient")

interface ITpRegisterClient {
    fun brukerHarTpForhold(
        fnr: String,
        orgnr: String,
        requestId: String
    ): Boolean?
}

/**
 * Kildekode: https://github.com/navikt/tp/blob/afc4d78b2923c5e38f9f07d617b1661761291ebd/src/main/kotlin/no/nav/samhandling/tp/controller/TjenestepensjonController.kt
 */
object TpRegisterClient : ITpRegisterClient {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.tp.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tp.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureM2MTokenProvider
    )

    override fun brukerHarTpForhold(
        fnr: String,
        orgnr: String,
        requestId: String
    ): Boolean? {
        require(orgnr.isNotBlank())
        require(fnr.isNotBlank())

        val uri = baseUri.resolve("/api/tjenestepensjon/hasForhold?orgnr=$orgnr")
        val httpRestClient = PostRequest(
            body = fnr,
            contentType = ContentType.TEXT_PLAIN,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-api"),
                Header("X-Request-Id", requestId)
            )
        )
        return try {
            client.post<String, TpRespons>(uri, httpRestClient)?.forhold.also {
                if (it == null) {
                    logger.info("Fikk null-respons fra tp-registeret. Orgnr: $orgnr")
                }
            }
        } catch (e: IkkeFunnetException) {
            logger.info("Person ikke funnet i TP-registeret, returnerer null. Melding: ${e.body}. Orgnr: $orgnr.")
            null
        } catch (e: ManglerTilgangException) {
            logger.warn("Manglet tilgang til å spørre TP-registeret. Melding: ${e.body}. Orgnr: $orgnr.")
            throw e
        }
    }
}

data class TpRespons(val forhold: Boolean)