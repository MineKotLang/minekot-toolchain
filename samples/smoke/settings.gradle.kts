rootProject.name = "smoke"

pluginManagement {
    includeBuild("../..")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
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
