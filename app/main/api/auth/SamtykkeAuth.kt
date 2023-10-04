package api.auth

import api.Config
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit


private const val PERSONIDENT_HEADER = "NAV-PersonIdent"
private const val SAMTYKKETOKEN_HEADER = "NAV-Samtykke-Token"

const val SAMTYKKE_AUTH_NAME = "samtykke"

private val logger = LoggerFactory.getLogger("SamtykkeAuth")

fun verifyJwt(token: String, consumerId:String, personIdent:String, config: Config):Boolean{
    val samtykkeJwks = SamtykkeJwks(config.oauth.samtykke.wellknownUrl)
    val jwkProvider = UrlJwkProvider(samtykkeJwks.jwksUri)

    val jwt = JWT.decode(token)
    val jwk = jwkProvider.get(jwt.keyId)

    //val publicKey: RSAPublicKey = jwk.publicKey as RSAPublicKey // unsafe
    val publicKey = jwk.publicKey as? RSAPublicKey ?: throw Exception("Invalid key type") // safe

    val algorithm = when (jwk.algorithm) {
        "RS256" -> Algorithm.RSA256(publicKey, null)
        "RSA-OAEP-256" -> Algorithm.RSA256(publicKey, null)
        else -> throw Exception("Unsupported algorithm")
    }
    logger.info("OfferedBy: ${personIdent}")
    logger.info("actual OfferedBy: ${jwt.getClaim("OfferedBy").asString()}")

    logger.info("CoveredBy: ${consumerId}")
    logger.info("actual CoveredBy: ${jwt.getClaim("CoveredBy").asString()}")


    val verifier = JWT.require(algorithm) // signature
        .withIssuer(samtykkeJwks.issuer)
        .withAudience("https://aap-test-token-provider.intern.dev.nav.no")
        .withClaim("CoveredBy", consumerId)
        .withClaim("OfferedBy", personIdent)
        .build()

    return try {
        verifier.verify(token)
        true
    } catch (e: Exception) {
        logger.info("Token not verified: $e")
        false
    }

}



private fun kravOmPersonidentErOppfylt(call: ApplicationCall, cred: JWTCredential): Boolean {
    val offeredBy = cred.payload.getClaim("OfferedBy")
    if (offeredBy.isMissing) {
        return false
    }

    return offeredBy.asString() == call.request.header(PERSONIDENT_HEADER)
}