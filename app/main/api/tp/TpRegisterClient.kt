package api.tp

import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

object TpRegisterClient {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.tpregister.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tpregister.scope"))
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    fun brukerHarTpForholdForOrgnr(
        fnr: String,
        orgnr: String,
        requestId: String
    ): Boolean {
        val uri = baseUri.resolve("/api/tjenestepensjon/hasForhold?orgnr=$orgnr")
        val httpRestClient = PostRequest(
            body = fnr,
            additionalHeaders = listOf(
                Header("Content-Type", "text/plain"),
                Header("Nav-Consumer-Id", "aap-api"),
                Header("Nav-Call-Id", requestId)
            )
        )
        return checkNotNull(client.post<String, TpRegisterResponse>(uri, httpRestClient)).forhold

    }

    private class TpRegisterResponse(
        val forhold: Boolean
    )

}