package api.auth

import api.util.Config
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("MaskinportenAuth")
const val MASKINPORTEN_AFP_PRIVAT = "fellesordning"
const val MASKINPORTEN_AFP_OFFENTLIG = "afp-offentlig"
const val MASKINPORTEN_TP_ORDNINGEN = "tp-ordningen"

fun ApplicationCall.hentConsumerId(): String {
    val principal = requireNotNull(this.principal<JWTPrincipal>())
    val consumer = requireNotNull(principal.payload.getClaim("consumer"))
    return consumer.asMap()["ID"].toString().split(":").last()
}

fun AuthenticationConfig.maskinporten(name: String, scope: List<String>, config: Config) {
    val maskinportenJwkProvider: JwkProvider = JwkProviderBuilder(config.oauth.maskinporten.jwksUri)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    jwt(name) {
        verifier(maskinportenJwkProvider, config.oauth.maskinporten.issuer.name)
        challenge { _, _ -> call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til maskinporten") }
        validate { cred ->
            if (!scope.contains(cred.getClaim("scope", String::class))) {
                logger.warn("Wrong scope in claim. Ser etter $scope, fikk ${cred.getClaim("scope", String::class)}")
                return@validate null
            }

            JWTPrincipal(cred.payload)
        }
    }
}
