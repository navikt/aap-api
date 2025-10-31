package api.util

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import java.net.URI
import java.net.URL


data class Config(
    val oauth: OauthConfig = OauthConfig(),
    val arenaoppslag: ArenaoppslagConfig = ArenaoppslagConfig(),
    val azure: AzureConfig = AzureConfig(
        tokenEndpoint = URI.create(requiredConfigForKey("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")),
        clientId = requiredConfigForKey("AZURE_APP_CLIENT_ID"),
        clientSecret = requiredConfigForKey("AZURE_APP_CLIENT_SECRET"),
        jwksUri = requiredConfigForKey("AZURE_OPENID_CONFIG_JWKS_URI"),
        issuer = requiredConfigForKey("AZURE_OPENID_CONFIG_ISSUER"),
    ),
    val kafka: KafkaConfig = KafkaConfig(),
    val sporingslogg: SporingsloggConfig = SporingsloggConfig(),
    val apiInternConfig: ApiInternConfig = ApiInternConfig()
)

data class KafkaConfig(
    val brokers: String = requiredConfigForKey("KAFKA_BROKERS"),
    val truststorePath: String = requiredConfigForKey("KAFKA_TRUSTSTORE_PATH"),
    val keystorePath: String = requiredConfigForKey("KAFKA_KEYSTORE_PATH"),
    val credstorePsw: String = requiredConfigForKey("KAFKA_CREDSTORE_PASSWORD"),
)

data class OauthConfig(
    val maskinporten: MaskinportenConfig = MaskinportenConfig(),
    val samtykke: SamtykkeConfig = SamtykkeConfig()
)

data class SporingsloggConfig(
    val enabled: Boolean = requiredConfigForKey("SPORINGSLOGG_ENABLED").toBoolean(),
    val topic: String = requiredConfigForKey("SPORINGSLOGG_TOPIC")
)

data class MaskinportenConfig(
    val jwksUri: URL = URI(requiredConfigForKey("MASKINPORTEN_JWKS_URI")).toURL(),
    val issuer: IssuerConfig = IssuerConfig(),
    val scope: ScopeConfig = ScopeConfig()
) {
    data class IssuerConfig(
        val name: String = requiredConfigForKey("MASKINPORTEN_ISSUER"),
        val discoveryUrl: String = requiredConfigForKey("MASKINPORTEN_WELL_KNOWN_URL"),
        val audience: String = requiredConfigForKey("AAP_AUDIENCE"),
        val optionalClaims: String = "sub,nbf",
    )

    data class ScopeConfig(
        val afpprivat: String = "nav:aap:afpprivat.read",
        val afpoffentlig: String = "nav:aap:afpoffentlig.read",
        val tpordningen: String = "nav:aap:tpordningen.read",
        val delegertTpOrdningen: String = "nav:aap/tpordningen.read",
        val afpoffentligAksio: String = "nav:aap/delegertafpoffentlig.read"
    )
}

data class ArenaoppslagConfig(
    val proxyBaseUrl: String = requiredConfigForKey("ARENAOPPSLAG_PROXY_BASE_URL"),
    val scope: String = requiredConfigForKey("ARENAOPPSLAG_SCOPE")
)

data class ApiInternConfig(
    val url: String = requiredConfigForKey("API_INTERN_URL"),
    val scope : String = requiredConfigForKey("API_INTERN_SCOPE")
)

data class SamtykkeConfig(
    val wellknownUrl: String = requiredConfigForKey("ALTINN_WELLKNOWN"),
    val audience: String = requiredConfigForKey("ALTINN_AUDIENCE")
)
