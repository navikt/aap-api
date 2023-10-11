package api.routes

import api.Config
import api.arena.ArenaoppslagRequest
import api.arena.ArenaoppslagResponse
import api.arena.ArenaoppslagRestClient
import api.sporingslogg.SporingsloggKafkaClient
import com.papsign.ktor.openapigen.model.Described
import com.papsign.ktor.openapigen.model.security.HttpSecurityScheme
import com.papsign.ktor.openapigen.model.security.SecuritySchemeModel
import com.papsign.ktor.openapigen.model.security.SecuritySchemeType
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import org.slf4j.LoggerFactory
import java.lang.Exception


private val logger = LoggerFactory.getLogger("VedtakRoutes")

fun NormalOpenAPIRoute.vedtak(arenaoppslagRestClient: ArenaoppslagRestClient, config: Config, sporingsloggKafkaClient: SporingsloggKafkaClient) {
    auth {
        route("/fellesordning/vedtak") {
            throws(HttpStatusCode.InternalServerError, Error("bad.request", mapOf<String, String>()), {ex: Exception -> Error(ex.message, ex.cause)}) {
                post<Unit, List<ArenaoppslagResponse>, ArenaoppslagRequest>(
                    info(summary = "Hent fil", description = "Hent ut en fil basert på filreferanse")
                ) { _, body ->
                    try {
                        logger.info("Incomming")
                        respond(arenaoppslagRestClient.hentVedtak(body))
                    } catch (e: Exception) {
                        logger.error("Feil i kall", e)
                        throw e
                    }
                }
            }
        }
        /*
        get("/dsop/test") {
            val samtykke = verifiserOgPakkUtSamtykkeToken(requireNotNull(call.request.header("NAV-samtykke-token")), call, config)
            logger.info("Samtykke OK: ${samtykke.samtykkeperiode}")
            sporingsloggKafkaClient.sendMelding(SporingsloggEntry(samtykke.personIdent,samtykke.consumerId,"aap", "behandlingsgrunnlag",
                LocalDateTime.now(),"leverteData",samtykke.samtykketoken,"dataForespoersel", "leverandoer"))
            call.respond("OK")
        }
        */
    }



}

data class Error<P>(val id: String, val payload: P)

val authProvider = JwtProvider();

inline fun NormalOpenAPIRoute.auth(route: OpenAPIAuthenticatedRoute<Principal>.() -> Unit): OpenAPIAuthenticatedRoute<Principal> {
    val authenticatedKtorRoute = this.ktorRoute.authenticate { }
    val openAPIAuthenticatedRoute =
        OpenAPIAuthenticatedRoute(authenticatedKtorRoute, this.provider.child(), authProvider = authProvider);
    return openAPIAuthenticatedRoute.apply {
        route()
    }
}

class JwtProvider : AuthProvider<Principal> {
    override val security: Iterable<Iterable<AuthProvider.Security<*>>> =
        listOf(
            listOf(
                AuthProvider.Security(
                    SecuritySchemeModel(
                        SecuritySchemeType.http,
                        scheme = HttpSecurityScheme.bearer,
                        bearerFormat = "JWT",
                        referenceName = "Maskinporten",
                    ), emptyList<Scopes>()
                )
            )
        )

    override suspend fun getAuth(pipeline: PipelineContext<Unit, ApplicationCall>): Principal {
        return pipeline.context.authentication.principal() ?: throw RuntimeException("No JWTPrincipal")
    }

    override fun apply(route: NormalOpenAPIRoute): OpenAPIAuthenticatedRoute<Principal> {
        val authenticatedKtorRoute = route.ktorRoute.authenticate { }
        return OpenAPIAuthenticatedRoute(authenticatedKtorRoute, route.provider.child(), this)
    }
}

enum class Scopes(override val description: String) : Described {
    Profile("Some scope")
}