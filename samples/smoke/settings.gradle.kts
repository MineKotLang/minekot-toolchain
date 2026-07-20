rootProject.name = "smoke"

pluginManagement {
    includeBuild("../..")
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("org.minekot:minekot-toolchain-lint-rules"))
            .using(project(":plugin:minekot-toolchain-lint-rules"))
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.minekot.org/releases")
        maven("https://maven.minekot.org/snapshots")
    }
}
