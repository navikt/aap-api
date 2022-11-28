import no.nav.aap.kafka.streams.KStreamsConfig

data class Config(
    val kafka: KStreamsConfig,
    val oauth: OauthConfig,
)

data class OauthConfig(
    val maskinporten: MaskinportenConfig
)

data class MaskinportenConfig(
    val jwksUri: String,
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