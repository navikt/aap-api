import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    id("aap.conventions")
    id("io.ktor.plugin") version "3.4.0"
    application
}

application {
    mainClass.set("api.AppKt")
}

val ktorVersion = "3.4.0"
val komponenterVersjon = "2.0.2"
val mockOAuth2ServerVersion = "3.0.1"
val apiInternVersjon = "0.0.25"

dependencies {
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
    constraints {
        implementation("io.netty:netty-common:4.2.10.Final")
        implementation("io.netty:netty-handler:4.2.10.Final")
    }
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-openapi-schema:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("com.auth0:java-jwt:4.5.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.0")
    implementation("com.nimbusds:nimbus-jose-jwt:10.7")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.3")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("org.apache.kafka:kafka-streams:4.2.0")
    implementation("io.prometheus:prometheus-metrics-tracer-initializer:1.5.0")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:1.21.0")
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