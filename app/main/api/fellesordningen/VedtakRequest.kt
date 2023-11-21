package api.fellesordningen

import java.time.LocalDate

data class VedtakRequest(
    val personIdent: String,
    val datoForOnsketUttakForAFP: LocalDate,
)