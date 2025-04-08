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
        require(fraOgMedDato <= tilOgMedDato) { PeriodeErrorException("fraOgMed må være større eller lik tilOgMed") }
    }
    fun tilVedtakRequest() = VedtakRequest(personidentifikator, fraOgMedDato, tilOgMedDato)
}

data class VedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate = LocalDate.now().plusYears(100)
) {
    init {
        require(fraOgMedDato <= tilOgMedDato) { PeriodeErrorException("fraOgMed må være større eller lik tilOgMed") }
    }
}