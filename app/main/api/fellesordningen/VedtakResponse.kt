package api.fellesordningen

import java.time.LocalDate

data class VedtakResponse(val fnr: String, val perioder: List<VedtakPeriode>)

data class VedtakPeriode(val fraDato:LocalDate, val tilDato:LocalDate)