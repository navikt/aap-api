import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    id("aap.conventions")
    id("io.ktor.plugin") version "3.5.1"
    application
}

application {
    mainClass.set("api.AppKt")
}

val ktorVersion = "3.5.1"
val jacksonVersion = "2.22.1"
val jackson3Version = "3.2.1"
val nettyVersion = "4.2.16.Final"

val komponenterVersjon = "2.0.107"
val mockOAuth2ServerVersion = "5.0.2"
val apiInternVersjon = "0.0.97"

dependencies {
    // Overstyr versjoner ktor setter, for å få sikkerhetsfikser
    implementation(platform("io.netty:netty-bom:$nettyVersion"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    // Overstyr versjoner logstash setter, for å få sikkerhetsfikser
    implementation(platform("tools.jackson:jackson-bom:$jackson3Version"))

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.api.intern:kontrakt:$apiInternVersjon")

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-openapi-schema:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.5.38")
    implementation("com.auth0:java-jwt:4.6.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.1")
    implementation("com.nimbusds:nimbus-jose-jwt:10.9.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.17.0")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("org.apache.kafka:kafka-streams:4.3.1")
    implementation("io.prometheus:prometheus-metrics-tracer-initializer:1.8.0")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:1.22.0")
    }
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    constraints {
        implementation("net.minidev:json-smart:2.6.0")
    }
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}
