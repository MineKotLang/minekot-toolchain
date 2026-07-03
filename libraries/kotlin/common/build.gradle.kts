plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.kotlin.stdlib)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-kt-common"
    }
}
