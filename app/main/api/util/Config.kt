package api.util

import jdk.jfr.Enabled
import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.ktor.client.AzureConfig
import java.net.URL

data class Config(
    val oauth: OauthConfig,
    val arenaoppslag: ArenaoppslagConfig,
    val azure: AzureConfig,
    val kafka: StreamsConfig,
    val sporingsloggConfig: SporingsloggConfig
)

data class OauthConfig(
    val maskinporten: MaskinportenConfig,
    val samtykke: SamtykkeConfig
)

data class SporingsloggConfig(
    val enabled: Boolean,
    val topic: String
)

data class MaskinportenConfig(
    val jwksUri: URL,
    val issuer: IssuerConfig,
    val scope: ScopeConfig
) {
    data class IssuerConfig(
        val name: String,
        val discoveryUrl: String,
        val audience: String,
        val optionalClaims: String,
    )

    data class ScopeConfig(
        val afpprivat: String
    )
}

data class ArenaoppslagConfig(
    val proxyBaseUrl:String,
    val scope: String
)

data class SamtykkeConfig(
    val wellknownUrl: String,
    val audience: String
)
