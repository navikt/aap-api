plugins {
    kotlin("jvm") version "1.8.10"
    id("io.ktor.plugin") version "2.2.4"
}

application {
    mainClass.set("AppKt")
}

val aapLibVersion = "3.6.30"
val ktorVersion = "2.2.4"

dependencies {
    implementation("com.github.navikt.aap-libs:kafka-2:$aapLibVersion")
    implementation("com.github.navikt.aap-libs:ktor-utils:$aapLibVersion")
    implementation("com.github.navikt.aap-vedtak:kafka-dto:1.1.1")

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-webjars:$ktorVersion")

    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.4")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")

    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("com.github.navikt.aap-libs:kafka-test-2:$aapLibVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:9.30.2")
    testImplementation(kotlin("test"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "19"
    }

    withType<Test> {
        useJUnitPlatform()
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
