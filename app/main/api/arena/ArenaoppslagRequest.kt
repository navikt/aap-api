package api.arena

import java.time.LocalDate

data class ArenaoppslagRequest(
    val personId: String,
    val datoForOnsketUttakForAFP: LocalDate,
)