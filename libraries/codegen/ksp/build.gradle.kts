plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":libraries:codegen:minekot-codegen-core"))
    api(libs.ksp.api)
    api(libs.kotlinpoet.ksp)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-ksp"
    }
}
