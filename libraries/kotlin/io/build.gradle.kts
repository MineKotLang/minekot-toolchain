plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":libraries:kotlin:minekot-kt-common"))
    api(libs.kotlinx.io.core)
    api(libs.kotlinx.io.bytestring)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-kt-io"
    }
}
