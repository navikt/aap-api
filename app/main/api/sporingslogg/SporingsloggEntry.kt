package api.sporingslogg

import java.time.LocalDateTime
//https://confluence.adeo.no/display/KES/Sporingslogg
data class SporingsloggEntry(
    val person: String,
    val mottaker: String,
    val tema: String,
    val behandlingsGrunnlag: String,
    val uthentingsTidspunkt: LocalDateTime,
    val leverteData: String,
    val samtykkeToken: String? = null,
    val dataForespoersel: String? = null,
    val leverandoer: String? = null
)