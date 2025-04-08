package api.afp

import api.util.PeriodeErrorException
import java.time.LocalDate

data class VedtakRequestMedSaksRef(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate = LocalDate.now().plusYears(100),
    val saksId: String
) {
    init {
        require(fraOgMedDato <= tilOgMedDato) { throw PeriodeErrorException("fraOgMed (${fraOgMedDato}) må være større eller lik tilOgMed(${tilOgMedDato})") }
    }
    fun tilVedtakRequest() = VedtakRequest(personidentifikator, fraOgMedDato, tilOgMedDato)
}

data class VedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate = LocalDate.now().plusYears(100)
) {
    init {
        require(fraOgMedDato <= tilOgMedDato) { throw PeriodeErrorException("fraOgMed (${fraOgMedDato}) må være større eller lik tilOgMed(${tilOgMedDato})") }
    }
}