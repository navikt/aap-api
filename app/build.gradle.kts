import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    id("aap.conventions")
    id("io.ktor.plugin") version "3.3.3"
    application
}

application {
    mainClass.set("api.AppKt")
}

val ktorVersion = "3.3.3"
val komponenterVersjon = "1.0.482"
val mockOAuth2ServerVersion = "3.0.1"
val apiInternVersjon = "0.0.22"

dependencies {
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")

    implementation("no.nav.aap.api.intern:kontrakt:$apiInternVersjon")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-common:4.2.9.Final")
        implementation("io.netty:netty-handler:4.2.9.Final")
    }
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-webjars:$ktorVersion")

    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.5.24")
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.1")
    implementation("com.nimbusds:nimbus-jose-jwt:10.7")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.1")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("org.apache.kafka:kafka-streams:4.1.1")
    implementation("io.prometheus:prometheus-metrics-tracer-initializer:1.4.3")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:1.20.0")
    }
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    constraints {
        implementation("net.minidev:json-smart:2.6.0")
    }
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        // Duplikate class og ressurs-filer kan skape runtime-feil, fordi JVM-en velger den første på classpath
        // ved duplikater, og det kan være noe annet enn vår kode (og libs vi bruker) forventer.
        // Derfor logger vi en advarsel hvis vi oppdager duplikater.
        duplicatesStrategy = DuplicatesStrategy.WARN

        mergeServiceFiles()

        filesMatching(listOf("META-INF/io.netty.*", "META-INF/services/**", "META-INF/maven/**")) {
            // For disse filene fra upstream, antar vi at de er identiske hvis de har samme navn.
            // Merk at META-INF/maven/org.webjars/swagger-ui/pom.properties
            // brukes av com.papsign.ktor.openapigen.SwaggerUIVersion
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            // Vi beholder alle pom.properties fra Maven for å støtte generering av SBOM i Nais
        }

        // Helt unødvendige filer som ofte skaper duplikater
        val fjernDisseDuplikatene = listOf(
            "*.SF", "*.DSA", "*.RSA", // Signatur-filer som ikke trengs på runtime
            "*NOTICE*", "*LICENSE*", "*DEPENDENCIES*", "*README*", "*COPYRIGHT*", // til mennesker bare
            "proguard/**", // Proguard-konfigurasjoner som ikke trengs på runtime
            "com.android.tools/**" // Android build-filer som ikke trengs på runtime
        )
        fjernDisseDuplikatene.forEach { pattern -> exclude("META-INF/$pattern") }
    }
}