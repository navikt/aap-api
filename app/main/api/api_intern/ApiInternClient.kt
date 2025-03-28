package api.api_intern

import api.Periode
import api.afp.VedtakRequestMedSaksRef
import api.util.ApiInternConfig
import no.nav.aap.arenaoppslag.kontrakt.ekstern.EksternVedtakRequest
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI
import java.util.*


// KLASSER FRA API INTERN - GJØR DISSE TIL KONTRAKT
data class VedtakUtenUtbetaling(
    val dagsats: Int,
    val vedtakId: String,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: String, //reg_dato
    val vedtaksTypeKode: String,
    val vedtaksTypeNavn: String,
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val kildesystem: String = "ARENA",
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
)

data class MediumFraInternApi(val vedtak: List<VedtakUtenUtbetaling>)

data class Vedtak(
    val dagsats: Int,
    val vedtakId: String,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: String, //reg_dato
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val kildesystem: Kilde = Kilde.ARENA,
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
    val vedtaksTypeKode: String,
    val vedtaksTypeNavn: String,
    val utbetaling: List<UtbetalingMedMer>,
)

data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: Float
)

data class UtbetalingMedMer(
    val reduksjon: Reduksjon? = null,
    val utbetalingsgrad: Int? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
)


enum class Kilde {
    ARENA,
    KELVIN
}

data class MaksimumFraInternAoi(
    val vedtak: List<Vedtak>,
)

data class PerioderResponse(val perioder: List<Periode>)
// SLUTT PÅ KLASSER FRA API INTERN

interface IApiInternClient {
    fun hentMedium(vedtakRequest: EksternVedtakRequest): MediumFraInternApi
    fun hentMaksimum(callId: String, vedtakRequest: EksternVedtakRequest): MaksimumFraInternAoi
    fun hentPerioder(callId: UUID, vedtakRequest: VedtakRequestMedSaksRef): PerioderResponse
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

    override fun hentMedium(vedtakRequest: EksternVedtakRequest): MediumFraInternApi {
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
            throw Exception("Feil ved forsøk på å hente Medium fra api-intern: ${e.message}")
        }
    }

    override fun hentMaksimum(
        callId: String,
        vedtakRequest: EksternVedtakRequest
    ): MaksimumFraInternAoi {
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
            throw Exception("Feil ved forsøk på å hente Maksimum fra api-intern: ${e.message}")
        }
    }

    override fun hentPerioder(
        callId: UUID,
        vedtakRequest: VedtakRequestMedSaksRef
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
            throw Exception("Feil ved forsøk på å hente Perioder fra api-intern: ${e.message}")
        }
    }
}