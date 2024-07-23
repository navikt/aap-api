package api.afp

import api.dsop.Periode
import java.time.LocalDate

data class VedtakResponse(val perioder: List<VedtakPeriode>)

data class VedtakPeriode(val fraOgMedDato:LocalDate, val tilOgMedDato:LocalDate?)

data class VedtakMaks(
    val utbetaling: List<Utbetaling?>?,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String, //hypotese sak_id
    val vedtaksdato: String, //reg_dato
    val periode: Periode,
    val rettighetType: String, ////aktivitetsfase //Aktfasekode
)

data class Utbetaling(
    val utbetalingsgrad: Utbetalingsgrad,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
)

data class Utbetalingsgrad(
    val kode: String,
    val termnavn: String //TODO: Denne må renskes sammen med øyvind
)