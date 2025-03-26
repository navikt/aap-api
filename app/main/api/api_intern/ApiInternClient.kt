package api.api_intern

import api.Medium
import api.Periode
import api.afp.VedtakRequestMedSaksRef
import api.util.ApiInternConfig
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.util.*
import java.net.URI
import no.nav.aap.komponenter.json.DefaultJsonMapper


interface IApiInternClient {
    fun hentMedium(vedtakRequest: EksternVedtakRequest): Medium
    fun hentMaksimum(callId: String, vedtakRequest: EksternVedtakRequest): api.Maksimum
    fun hentPerioder(callId: UUID, vedtakRequest: VedtakRequestMedSaksRef): List<Periode>
}

class ApiInternClient(
    private val apiInternConfig: ApiInternConfig
) : IApiInternClient {
    private val uri = apiInternConfig.url
    private val config = ClientConfig(scope = apiInternConfig.scope)

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun hentMedium(vedtakRequest: EksternVedtakRequest): api.Medium{
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
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }))
        } catch (e: Exception) {
            throw Exception("Feil ved forsøk på å hente Medium fra api-intern: ${e.message}")
        }
    }

    override fun hentMaksimum(callId: String, vedtakRequest: EksternVedtakRequest): api.Maksimum {
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
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }))
        } catch (e: Exception) {
            throw Exception("Feil ved forsøk på å hente Maksimum fra api-intern: ${e.message}")
        }
    }

    override fun hentPerioder(callId: UUID, vedtakRequest: VedtakRequestMedSaksRef): List<api.Periode> {
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
                    mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }))
        } catch (e: Exception) {
            throw Exception("Feil ved forsøk på å hente Perioder fra api-intern: ${e.message}")
        }
    }
}