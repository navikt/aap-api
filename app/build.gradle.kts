import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.3"
    application
}

application {
    mainClass.set("api.AppKt")
}

val aapLibVersion = "5.0.25"
val ktorVersion = "3.0.3"
val komponenterVersjon = "1.0.101"

dependencies {
    implementation("com.github.navikt.aap-libs:ktor-auth:$aapLibVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")

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
        implementation("io.netty:netty-common:4.1.116.Final")
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

    implementation("no.nav.aap.arenaoppslag:kontrakt:0.0.16")

    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("com.nimbusds:nimbus-jose-jwt:9.47")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.3")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("org.apache.kafka:kafka-streams:3.9.0")
    implementation("io.prometheus:prometheus-metrics-tracer-initializer:1.3.5")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
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
