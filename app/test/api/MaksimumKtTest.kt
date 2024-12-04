package api

import no.nav.aap.arenaoppslag.kontrakt.modeller.AnnenReduksjon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MaksimumKtTest {
    @Test
    fun `hvis sent meldekort er false, returner 1 dag annen reduksjon`() {
        val tilKontrakt = AnnenReduksjon(
            sykedager = 1F,
            sentMeldekort = true,
            fraver = 2F
        ).fraKontrakt()

        assertEquals(tilKontrakt.sentMeldekort, 1)
    }
}