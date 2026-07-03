plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":libraries:kotlin:minekot-kt-common"))
    api(libs.junit.jupiter)
    api(libs.kotlinx.coroutines.test)
    api(libs.mockk)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-kt-testing"
    }
}
