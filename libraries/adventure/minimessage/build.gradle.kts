plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":libraries:adventure:minekot-adv-common"))
    api(libs.adventure.minimessage)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-adv-minimessage"
    }
}
