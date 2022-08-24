package vedtak

import java.time.LocalDate
import java.util.*

data class VedtakKafkaDto(
    val vedtaksid: UUID,
    val innvilget: Boolean,
    val grunnlagsfaktor: Double,
    val vedtaksdato: LocalDate,
    val virkningsdato: LocalDate,
    val fødselsdato: LocalDate
)
