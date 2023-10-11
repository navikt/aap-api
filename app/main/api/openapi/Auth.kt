package api.openapi

import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.server.auth.*

val openApiJwtProvider = OpenApiJwtProvider()

fun NormalOpenAPIRoute.openAPIAuthenticatedRoute(route: OpenAPIAuthenticatedRoute<Principal>.() -> Unit): OpenAPIAuthenticatedRoute<Principal> {
    val authenticatedKtorRoute = this.ktorRoute.authenticate { }
    val openAPIAuthenticatedRoute =
        OpenAPIAuthenticatedRoute(authenticatedKtorRoute, this.provider.child(), authProvider = openApiJwtProvider)
    return openAPIAuthenticatedRoute.apply {
        route()
    }
}