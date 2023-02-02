plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
}

application {
    mainClass.set("AppKt")
}

val aapLibVersion = "3.5.44"
val ktorVersion = "2.1.3"

dependencies {
    implementation("com.github.navikt.aap-libs:kafka:$aapLibVersion")
    implementation("com.github.navikt.aap-libs:ktor-utils:$aapLibVersion")
    implementation("com.github.navikt:aap-vedtak:1.0.139")

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-webjars:$ktorVersion")

    runtimeOnly("org.webjars:swagger-ui:4.15.0")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.0")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")

    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("com.github.navikt.aap-libs:kafka-test:$aapLibVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:9.25.6")
    testImplementation(kotlin("test"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")
