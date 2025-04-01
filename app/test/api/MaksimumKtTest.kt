package api

import no.nav.aap.arenaoppslag.kontrakt.modeller.AnnenReduksjon
import no.nav.aap.arenaoppslag.kontrakt.modeller.Reduksjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MaksimumKtTest {
    @Test
    fun `hvis sent meldekort er false, returner 1 dag annen reduksjon`() {
        val tilKontrakt = AnnenReduksjon(
            sykedager = 1F, sentMeldekort = true, fraver = 2F
        ).fraKontrakt()

        assertEquals(tilKontrakt.sentMeldekort, 1)
    }

    @Test
    fun `annen reduksjon er sum av verdiene fra AnnenReduksjon fra arenaoppslag`() {
        val tilKontrakt = Reduksjon(
            timerArbeidet = 3.0, annenReduksjon = AnnenReduksjon(
                sykedager = 1F, sentMeldekort = true, fraver = 2F
            )
        ).fraKontrakt()

        assertEquals(tilKontrakt.annenReduksjon, 4.0F)
    }

    @Test
    fun `parse dato til localdate uten t`() {
        val dato = "2021-01-13 00:00:00"

        val res = localDate(dato)

        assertThat(res).isEqualTo("2021-01-13")
    }


    @Test
    fun `parse dato til localdate med t`() {
        val dato = "2021-01-13T00:00:00"

        val res = localDate(dato)

        assertThat(res).isEqualTo("2021-01-13")
    }

    @Test
    fun `parse dato til dato`() {
        val dato = "2021-01-13"

        val res = localDate(dato)

        assertThat(res).isEqualTo("2021-01-13")
    }
}