plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":plugin:minekot-toolchain-lint-rules"))
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.atomicfu)
    implementation(libs.bundles.adventure)
    implementation(libs.commonmark)
    implementation(libs.commonmark.tables)
    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
}

gradlePlugin {
    plugins {
        create("minekotToolchain") {
            id = "org.minekot.toolchain"
            implementationClass = "org.minekot.toolchain.MineKotToolchainPlugin"
            displayName = "MineKot Toolchain"
            description = "MineKot project conventions, dependencies, publishing, and lint."
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = if (name == "pluginMaven") {
            "minekot-toolchain-gradle-plugin"
        } else {
            artifactId
        }
    }
}
