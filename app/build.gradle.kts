import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    id("aap.conventions")
    alias(kelvinLibs.plugins.ktor)
    application
}

application {
    mainClass.set("api.AppKt")
}

dependencies {
    // Overstyr versjoner ktor setter, for å få sikkerhetsfikser
    implementation(platform(libs.nettyBom))
    implementation(platform(libs.jacksonBom))
    // Overstyr versjoner logstash setter, for å få sikkerhetsfikser
    implementation(platform(libs.jackson3Bom))

    implementation(kelvinLibs.ktor.serialization.jackson)

    implementation(libs.kelvinHttpklient)
    implementation(libs.kelvinInfrastructure)
    implementation(libs.apiInternKontrakt)

    implementation(kelvinLibs.ktor.server.auth)
    implementation(kelvinLibs.ktor.server.auth.jwt)
    implementation(kelvinLibs.ktor.server.call.logging)
    implementation(kelvinLibs.ktor.server.call.id)
    implementation(kelvinLibs.ktor.server.content.negotiation)
    implementation(kelvinLibs.ktor.server.core)
    implementation(kelvinLibs.ktor.server.metrics.micrometer)
    implementation(kelvinLibs.ktor.server.netty)
    implementation(kelvinLibs.ktor.server.status.pages)
    implementation("io.ktor:ktor-server-swagger:${kelvinLibs.versions.ktor.get()}")
    implementation("io.ktor:ktor-openapi-schema:${kelvinLibs.versions.ktor.get()}")
    implementation(kelvinLibs.ktor.server.cors)

    implementation(kelvinLibs.ktor.client.cio)
    implementation(kelvinLibs.ktor.client.content.negotiation)

    implementation(kelvinLibs.logback.classic)
    implementation(libs.javaJwt)
    implementation(kelvinLibs.jackson.datatype.jsr310)
    implementation(kelvinLibs.micrometer.prometheus)
    implementation(kelvinLibs.logstash.logback.encoder)
    implementation(libs.kafkaStreams)
    implementation(libs.prometheusMetricsTracerInitializer)

    testImplementation(kelvinLibs.ktor.server.test.host)
    testImplementation(kelvinLibs.mock.oauth2.server)
    testImplementation(kelvinLibs.nimbus.jose.jwt)
    testImplementation(kelvinLibs.assertj.core)
    testImplementation(kelvinLibs.junit.jupiter.params)
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}
