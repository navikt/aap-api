package api.fellesordningen

import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import java.util.UUID

data class VedtakParams(@HeaderParam("Unik call id som brukes til logging") val `x-callid`: UUID)
