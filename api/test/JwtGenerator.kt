import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.*
import java.util.concurrent.TimeUnit

object JwtGenerator {

    private val jwkSet: JWKSet get() = JWKSet.parse(this::class.java.getResource("/jwkset.json")!!.readText())
    private val rsaKey: RSAKey get() = jwkSet.getKeyByKeyId("localhost-signer") as RSAKey

    fun generateToken(): SignedJWT = createSignedJWT(rsaKey, claims())

    private fun createSignedJWT(rsaJwk: RSAKey, claimsSet: JWTClaimsSet): SignedJWT {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.keyID).type(JOSEObjectType.JWT).build()
        val signer: JWSSigner = RSASSASigner(rsaJwk.toPrivateKey())
        return SignedJWT(header, claimsSet).apply {
            sign(signer)
        }
    }

    private fun claims(now: Date = Date()) = JWTClaimsSet.Builder()
        .subject(null)
        .issuer("maskinporten")
        .audience("aap")
        .jwtID(UUID.randomUUID().toString())
        .claim("scopes", "nav:aap:vedtak.read")
        .notBeforeTime(now)
        .issueTime(now)
        .expirationTime(Date(now.time + TimeUnit.MINUTES.toMillis((60 * 60 * 3600).toLong()))).build()

}