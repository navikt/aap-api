import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20"
    id("io.ktor.plugin") version "3.3.1"
    application
}

application {
    mainClass.set("api.AppKt")
}

val ktorVersion = "3.3.1"
val komponenterVersjon = "1.0.397"
val mockOAuth2ServerVersion = "3.0.0"
val apiInternVersjon = "0.0.19"

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
        implementation("io.netty:netty-common:4.2.7.Final")
        implementation("io.netty:netty-handler:4.2.7.Final")
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

    implementation("ch.qos.logback:logback-classic:1.5.19")
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")
    implementation("com.nimbusds:nimbus-jose-jwt:10.5")
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.4")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("org.apache.kafka:kafka-streams:4.1.0")
    implementation("io.prometheus:prometheus-metrics-tracer-initializer:1.4.1")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:1.19.0")
    }
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    constraints {
        implementation("net.minidev:json-smart:2.6.0")
    }
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation(kotlin("test"))
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")
