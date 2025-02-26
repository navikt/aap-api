package api.tp

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.ContentType
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI

private val logger = LoggerFactory.getLogger("api.tp.TpRegisterClient")

interface ITpRegisterClient {
    fun brukerHarTpForholdOgYtelse(
        fnr: String,
        orgnr: String,
        requestId: String
    ): Boolean?
}

object TpRegisterClient : ITpRegisterClient {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.tp.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tp.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    override fun brukerHarTpForholdOgYtelse(
        fnr: String,
        orgnr: String,
        requestId: String
    ): Boolean? {
        require(orgnr.isNotBlank())
        require(fnr.isNotBlank())

        val uri = baseUri.resolve("/api/tjenestepensjon/hasYtelse?orgnr=$orgnr")
        val httpRestClient = PostRequest(
            body = fnr,
            contentType = ContentType.TEXT_PLAIN,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-api"),
                Header("X-Request-Id", requestId)
            )
        )
        return try {
            client.post<String, Boolean>(uri, httpRestClient)
        } catch (e: IkkeFunnetException) {
            logger.info("Person ikke funnet i TP-registeret, returnerer null. Melding: ${e.body}. Orgnr: $orgnr.")
            null
        } catch (e: ManglerTilgangException) {
            logger.warn("Manglet tilgang til å spørre TP-registeret. Melding: ${e.body}. Orgnr: $orgnr.")
            throw e
        }
    }
}