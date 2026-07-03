plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":libraries:kotlin:minekot-kt-common"))
    api(libs.kotlinx.atomicfu)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-kt-atomic"
    }
}
