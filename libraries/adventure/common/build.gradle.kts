plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":libraries:kotlin:minekot-kt-common"))
    api(libs.bundles.adventure)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-adv-common"
    }
}
