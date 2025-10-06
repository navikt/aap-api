package api.api_intern

import api.afp.VedtakRequest
import api.util.ApiInternConfig
import no.nav.aap.api.intern.InternVedtakRequestApiIntern
import no.nav.aap.api.intern.Medium
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI
import java.util.*


interface IApiInternClient {
    fun hentMedium(vedtakRequest: InternVedtakRequestApiIntern): Medium
    fun hentMaksimum(callId: String, vedtakRequest: InternVedtakRequestApiIntern): no.nav.aap.api.intern.Maksimum
    fun hentPerioder(callId: UUID, vedtakRequest: VedtakRequest): PerioderResponse
}

class ApiInternClient(
    apiInternConfig: ApiInternConfig
) : IApiInternClient {
    private val uri = apiInternConfig.url
    private val config = ClientConfig(scope = apiInternConfig.scope)

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun hentMedium(vedtakRequest: InternVedtakRequestApiIntern): Medium {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = vedtakRequest
        )

        try {
            return requireNotNull(
                client.post(
                    uri = URI.create(uri).resolve("/maksimumUtenUtbetaling"),
                    request = request,
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) })
            )
        } catch (e: Exception) {
            throw Exception("Feil ved forsøk på å hente Medium fra api-intern", e)
        }
    }

    override fun hentMaksimum(
        callId: String,
        vedtakRequest: InternVedtakRequestApiIntern
    ): no.nav.aap.api.intern.Maksimum {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = vedtakRequest
        )

        try {
            return requireNotNull(
                client.post(
                    uri = URI.create(uri).resolve("/maksimum"),
                    request = request,
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) })
            )
        } catch (e: Exception) {
            throw Exception("Feil ved forsøk på å hente Maksimum fra api-intern", e)
        }
    }

    override fun hentPerioder(
        callId: UUID,
        vedtakRequest: VedtakRequest
    ): PerioderResponse {
        val request = PostRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json"),
            ),
            body = vedtakRequest
        )

        try {
            return requireNotNull(
                client.post(
                    uri = URI.create(uri).resolve("/perioder"),
                    request = request,
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) })
            )
        } catch (e: Exception) {
            throw Exception("Feil ved forsøk på å hente Perioder fra api-intern", e)
        }
    }
}