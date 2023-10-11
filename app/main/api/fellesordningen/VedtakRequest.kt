package api.fellesordningen

import com.papsign.ktor.openapigen.annotations.mapping.OpenAPIName
import java.time.LocalDate

data class VedtakRequest(
    @OpenAPIName("Personidentifikator")
    val personId: String,
    val datoForOnsketUttakForAFP: LocalDate,
)