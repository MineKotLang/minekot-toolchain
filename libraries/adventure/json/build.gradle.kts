plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":libraries:adventure:minekot-adv-common"))
    api(libs.adventure.json)
    runtimeOnly(libs.adventure.gson) {
        isTransitive = false
    }
    testRuntimeOnly(libs.adventure.gson)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-adv-json"
    }
}
