plugins {
    id("io.ktor.plugin")
    application
}

application {
    mainClass.set("AppKt")
}

val aapLibVersion = "3.1.11"
val ktorVersion = "2.1.0"

dependencies {
    implementation("com.github.navikt.aap-libs:kafka:$aapLibVersion")
    implementation("com.github.navikt.aap-libs:ktor-utils:$aapLibVersion")
    implementation("com.github.navikt:aap-vedtak:1.0.17")

    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.4")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")

    testImplementation("com.github.navikt.aap-libs:kafka-test:$aapLibVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(kotlin("test"))
}
