rootProject.name = "smoke"

pluginManagement {
    includeBuild("../..")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
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
        mavenCentral()
        mavenLocal()
        maven("https://maven.minekot.org/releases")
        maven("https://maven.minekot.org/snapshots")
    }
}
