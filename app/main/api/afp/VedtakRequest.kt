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
        if (fraOgMedDato > tilOgMedDato) {
            throw PeriodeErrorException(
                "fraOgMedDato (${fraOgMedDato}) må være mindre eller lik tilOgMedDato (${tilOgMedDato})"
            )
        }
    }

    fun tilVedtakRequest() = VedtakRequest(personidentifikator, fraOgMedDato, tilOgMedDato)
}

data class VedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate = LocalDate.now().plusYears(100)
) {
    init {
        if (fraOgMedDato > tilOgMedDato) {
            throw PeriodeErrorException(
                "fraOgMedDato (${fraOgMedDato}) må være mindre eller lik tilOgMedDato (${tilOgMedDato})"
            )
        }
    }
}