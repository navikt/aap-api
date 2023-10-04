package api.auth

import api.Config
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.slf4j.LoggerFactory
import java.security.interfaces.RSAPublicKey
import java.time.LocalDate



private val logger = LoggerFactory.getLogger("SamtykkeAuth")

data class Samtykkeperiode(val fraOgMed:LocalDate, val tilOgMed:LocalDate)
class SamtykkeIkkeGittException: Exception("Samtykke ikke gitt")

fun hentConsumerId(call:ApplicationCall):String{
    return call.principal<JWTPrincipal>()?.payload?.getClaim("consumer")?.asMap()?.get("ID").toString().split(":").last()
}

fun verifyJwt(token: String, call:ApplicationCall, config: Config):Samtykkeperiode{
    val samtykkeJwks = SamtykkeJwks(config.oauth.samtykke.wellknownUrl)
    val jwkProvider = UrlJwkProvider(samtykkeJwks.jwksUri)

    val consumerId = hentConsumerId(call)
    val personIdent = call.request.headers["NAV-PersonIdent"]?: ""

    val jwt = JWT.decode(token)
    val jwk = jwkProvider.get(jwt.keyId)

    //val publicKey: RSAPublicKey = jwk.publicKey as RSAPublicKey // unsafe
    val publicKey = jwk.publicKey as? RSAPublicKey ?: throw Exception("Invalid key type") // safe

    val algorithm = when (jwk.algorithm) {
        "RS256" -> Algorithm.RSA256(publicKey, null)
        "RSA-OAEP-256" -> Algorithm.RSA256(publicKey, null)
        else -> throw Exception("Unsupported algorithm")
    }


    val verifier = JWT.require(algorithm) // signature
        .withIssuer(samtykkeJwks.issuer)
        .withAudience("https://aap-test-token-provider.intern.dev.nav.no")
        .withClaim("CoveredBy", consumerId)
        .withClaim("OfferedBy", personIdent)
        .build()

    return try {
        verifier.verify(token)
        parseDates(jwt)
    } catch (e: Exception) {
        logger.info("Token not verified: $e")
        throw SamtykkeIkkeGittException()
    }

}

private fun parseDates(jwt:DecodedJWT):Samtykkeperiode{
    val services = jwt.getClaim("Services").asArray(String::class.java)
    return Samtykkeperiode(
        LocalDate.parse(services[1].split("=").last()),
        LocalDate.parse(services[2].split("=").last())
    )
}
