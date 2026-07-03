plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":libraries:kotlin:minekot-kt-common"))
    api(project(":libraries:kotlin:minekot-kt-io"))
    api(libs.kotlinx.serialization.json)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-kt-serialization"
    }
}
