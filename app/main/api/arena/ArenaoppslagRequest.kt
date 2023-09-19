package api.arena

import java.time.LocalDate

data class ArenaoppslagRequest(
    val fnr:String,
    val datoForOnsketUttakForAFP:LocalDate,
)