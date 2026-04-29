package api

import java.time.LocalDate

enum class Kilde {
    ARENA, KELVIN
}

fun no.nav.aap.api.intern.Kilde.tilKilde() = when (this) {
    no.nav.aap.api.intern.Kilde.ARENA -> Kilde.ARENA
    no.nav.aap.api.intern.Kilde.KELVIN -> Kilde.KELVIN
}


data class Maksimum(
    val vedtak: List<Vedtak>,
)

data class Medium(val vedtak: List<VedtakUtenUtbetaling>)

/**
 * @param saksnummer sak_id
 */
data class Vedtak(
    val dagsats: Int,
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtaksdato: LocalDate,
    val periode: Periode,
    val rettighetsType: String,
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val barnetillegg: Int,
    val kildesystem: Kilde,
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
    val vedtaksTypeKode: String?,
    val vedtaksTypeNavn: String?,
    val utbetaling: List<UtbetalingMedMer>,
)

data class VedtakUtenUtbetaling(
    val dagsats: Int,
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtaksdato: LocalDate,
    val vedtaksTypeKode: String?,
    val vedtaksTypeNavn: String?,
    val periode: Periode,
    val rettighetsType: String,
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val barnetillegg: Int,
    val kildesystem: Kilde,
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
)


data class UtbetalingMedMer(
    val reduksjon: Reduksjon? = null,
    val utbetalingsgrad: Int? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
    val barnetillegg: Int
)

data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: Double
)

data class Periode(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?
)

