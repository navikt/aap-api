package api.auth

import api.Config
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private const val PERSONIDENT_HEADER = "NAV-PersonIdent"
private const val SAMTYKKETOKEN_HEADER = "NAV-Samtykke-Token"

const val SAMTYKKE_AUTH_NAME = "samtykke"

private val logger = LoggerFactory.getLogger("SamtykkeAuth")

fun AuthenticationConfig.samtykke(config: Config) {
    val samtykkeJwks = SamtykkeJwks(config.oauth.samtykke.wellknownUrl)
    logger.info("Issuer: ${samtykkeJwks.issuer}")
    val samtykkeJwkProvider: JwkProvider = JwkProviderBuilder(samtykkeJwks.jwksUri)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    jwt(SAMTYKKE_AUTH_NAME) {
        authHeader { call ->
            val samtykkeHeader = requireNotNull(call.request.header(SAMTYKKETOKEN_HEADER)) { "Samtykke-header mangler" }
            parseAuthorizationHeader(samtykkeHeader)
        }

        verifier(samtykkeJwkProvider, samtykkeJwks.issuer)
        challenge { _, _ -> call.respond(HttpStatusCode.Unauthorized, "Samtykke ikke gitt") }
        validate { cred ->
            if (!kravOmPersonidentErOppfylt(this, cred)) {
                logger.error("Personident ${this.request.header(PERSONIDENT_HEADER)} stemmer ikke med ${cred.payload.getClaim("OfferedBy")}")
                return@validate null
            }
            JWTPrincipal(cred.payload)
        }
    }
}

private fun kravOmPersonidentErOppfylt(call: ApplicationCall, cred: JWTCredential): Boolean {
    val offeredBy = cred.payload.getClaim("OfferedBy")
    if (offeredBy.isMissing) {
        return false
    }

    return offeredBy.asString() == call.request.header(PERSONIDENT_HEADER)
}