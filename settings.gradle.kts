rootProject.name = "api"

include(
    "app",
)

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    dependencyResolutionManagement {
        versionCatalogs {
            create("kelvinLibs") {
                from("no.nav.aap.kelvin:version-catalog:2.0.160")
            }
        }
    }
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
        mavenLocal()
    }
}
