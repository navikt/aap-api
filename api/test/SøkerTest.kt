import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SøkerTest {

    @Test
    fun `søker route accessible`() {
        Mocks().use { mocks ->
            testApplication {
                environment { config = mocks.environmentVariables }
                application { api() }

                val response = client.get("/soker/1234")
                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }
}
