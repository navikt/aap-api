package api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MaksimumKtTest {

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