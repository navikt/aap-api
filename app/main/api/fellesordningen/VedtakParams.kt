package api.fellesordningen

import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import java.util.UUID

data class VedtakParams(@HeaderParam("Unik call id (uuid) som brukes til logging. Settes av den som kaller.") val `x-callid`: UUID)
