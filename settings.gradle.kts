rootProject.name = TODO("Replace with a string value of the project name")

pluginManagement.repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
}


plugins {
    id("com.gradle.develocity") version "4.5.0"
}

develocity {
    buildScan {
        termsOfUseUrl = ("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree = "yes"
        publishing.onlyIf { true }
    }
}
