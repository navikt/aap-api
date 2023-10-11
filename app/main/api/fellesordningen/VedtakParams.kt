package api.fellesordningen

import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import java.util.UUID

data class VedtakParams(@HeaderParam("X-CallId") val callId: UUID)
