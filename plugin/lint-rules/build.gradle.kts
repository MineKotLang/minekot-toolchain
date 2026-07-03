plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly(libs.detekt.api)
    testImplementation(libs.detekt.test)
    testImplementation(libs.junit.jupiter)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-toolchain-lint-rules"
    }
}
