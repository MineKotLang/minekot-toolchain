plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly(libs.detekt.api)
    api(libs.detekt.formatting)
    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test.utils)
    testRuntimeOnly(libs.detekt.psi.utils)
    testImplementation(libs.junit.jupiter)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "minekot-toolchain-lint-rules"
    }
}
