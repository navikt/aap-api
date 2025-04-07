package api.afp

import java.time.LocalDate

data class VedtakRequestMedSaksRef(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val saksId: String
) {
    init {
        require(fraOgMedDato <= tilOgMedDato) { "fraOgMedDato må være før eller lik tilOgMedDato" }
    }
    fun tilVedtakRequest() = VedtakRequest(personidentifikator, fraOgMedDato, tilOgMedDato)
}

data class VedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate
) {
    init {
        require(fraOgMedDato <= tilOgMedDato) { "fraOgMedDato må være før eller lik tilOgMedDato" }
    }
}