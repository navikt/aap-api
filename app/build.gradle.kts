import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    id("aap.conventions")
    alias(libs.plugins.ktor)
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

    implementation(libs.ktorSerializationJackson)

    implementation(libs.kelvinHttpklient)
    implementation(libs.kelvinInfrastructure)
    implementation(libs.apiInternKontrakt)

    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerAuthJwt)
    implementation(libs.ktorServerCallLogging)
    implementation(libs.ktorServerCallId)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerMetricsMicrometer)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerStatusPages)

    implementation(libs.ktorServerSwagger)
    implementation(libs.ktorOpenapiSchema)
    implementation(libs.ktorServerCors)

    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)

    implementation(libs.logback)
    implementation(libs.javaJwt)
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.nimbusJoseJwt)
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.kafkaStreams)
    implementation(libs.prometheusMetricsTracerInitializer)

    testImplementation(libs.ktorServerTestHost)
    constraints {
        implementation(libs.commonsCodec)
    }
    testImplementation(libs.mockOAuth2Server)
    constraints {
        implementation(libs.jsonSmart)
    }
    testImplementation(libs.assertj)
    testImplementation(libs.junitJupiterParams)
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}
