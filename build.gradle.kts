plugins {
    kotlin("jvm") version "1.8.10"
    id("io.ktor.plugin") version "2.2.3" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

subprojects {
    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "19"
        }

        withType<Test> {
            useJUnitPlatform()
        }
    }
}
