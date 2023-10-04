package api

import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.ktor.client.AzureConfig
import java.net.URL

data class Config(
    val oauth: OauthConfig,
    val arenaoppslag: ArenaoppslagConfig,
    val azure: AzureConfig,
    val kafka: StreamsConfig
)

data class OauthConfig(
    val maskinporten: MaskinportenConfig,
    val samtykke: SamtykkeConfig
)

data class MaskinportenConfig(
    val jwksUri: URL,
    val issuer: IssuerCionfig,
    val scope: ScopeConfig
) {
    data class IssuerCionfig(
        val name: String,
        val discoveryUrl: String,
        val audience: String,
        val optionalClaims: String,
    )

    data class ScopeConfig(
        val vedtak: String
    )
}

data class ArenaoppslagConfig(
    val proxyBaseUrl:String,
    val scope: String
)

data class SamtykkeConfig(
    val wellknownUrl: String
)
