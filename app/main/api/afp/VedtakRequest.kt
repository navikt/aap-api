package api.afp

import java.time.LocalDate

data class VedtakRequestMedSaksRef(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val saksId: String?
) {
    fun toVedtakRequest() = VedtakRequest(
        personidentifikator = personidentifikator,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato
    )
}


data class VedtakRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate
)
