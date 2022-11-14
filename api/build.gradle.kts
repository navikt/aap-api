plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
}

application {
    mainClass.set("AppKt")
}

val aapLibVersion = "3.5.22"
val ktorVersion = "2.1.2"

dependencies {
    implementation("com.github.navikt.aap-libs:kafka:$aapLibVersion")
    implementation("com.github.navikt.aap-libs:ktor-utils:$aapLibVersion")
    implementation("com.github.navikt:aap-vedtak:1.0.139")

    implementation("com.auth0:jwks-rsa:0.17.0")
    implementation("com.nimbusds:nimbus-jose-jwt:9.25")

    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.4.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.4")
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.4")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")

    implementation("io.ktor:ktor-server-webjars:$ktorVersion")
    runtimeOnly("org.webjars:swagger-ui:4.15.0")

    testImplementation("com.github.navikt.aap-libs:kafka-test:$aapLibVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(kotlin("test"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")
