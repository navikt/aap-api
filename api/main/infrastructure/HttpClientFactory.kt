package infrastructure

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.jackson.*
import org.slf4j.LoggerFactory

object HttpClientFactory {
    fun create() = HttpClient(CIO) {
        install(ContentNegotiation) { jackson { registerModule(JavaTimeModule()) } }
        install(HttpRequestRetry)
        install(HttpTimeout)
        install(Logging) {
            level = LogLevel.ALL
            logger = HttpClientLogger(LoggerFactory.getLogger("secureLog"))
        }
    }
}

internal class HttpClientLogger(private val log: org.slf4j.Logger) : Logger {
    override fun log(message: String) = log.info(message)
}
