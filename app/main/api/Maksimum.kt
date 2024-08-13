package api

import java.time.LocalDate


data class Maksimum1 (
    val vedtak: List<Vedtak>
)

data class Maksimum2(
    val vedtak: List<Vedtak>,
    val utbetalinger: List<UtbetalingMedMer>,
)

data class Vedtak(
    val utbetaling: List<UtbetalingMedMer>,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String, //hypotese sak_id
    val vedtaksdato: String, //reg_dato
    val periode: Periode,
    val rettighetType: String, ////aktivitetsfase //Aktfasekode
)

data class UtbetalingMedMer(
    val utbetalingsgrad: Utbetalingsgrad? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
)

data class Utbetalingsgrad(
    val kode: String,
    val termnavn: String //TODO: Denne må renskes sammen med øyvind
)

data class Periode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?
)