package api

import java.time.LocalDate


data class Maksimum(
    val vedtak: List<Vedtak>,
)

/**
 * @param status Hypotese, vedtaksstatuskode
 * @param saksnummer hypotese sak_id
 */
data class Vedtak(
    val utbetaling: List<UtbetalingMedMer>,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: String, //reg_dato
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int
)

data class UtbetalingMedMer(
    val reduksjon: Reduksjon? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
)

data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: AnnenReduksjon
)

data class AnnenReduksjon(
    val sykedager: Float?,
    val sentMeldekort:Boolean?,
    val fraver: Float?
)

data class Periode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?
)