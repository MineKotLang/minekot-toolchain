@file:Suppress("UnstableApiUsage")

rootProject.name = "minekot-toolchain"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.minekot.org/releases")
        maven("https://maven.minekot.org/snapshots")
    }
}

plugins {
    id("com.gradle.develocity") version "4.5.0"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
        publishing.onlyIf { true }
    }
}

val projects = mapOf(
    ":plugin:toolchain" to "minekot-toolchain-gradle-plugin",
    ":plugin:lint-rules" to "minekot-toolchain-lint-rules",
    ":libraries:kotlin:common" to "minekot-kt-common",
    ":libraries:kotlin:reflection" to "minekot-kt-reflection",
    ":libraries:kotlin:serialization" to "minekot-kt-serialization",
    ":libraries:kotlin:io" to "minekot-kt-io",
    ":libraries:kotlin:coroutines" to "minekot-kt-coroutines",
    ":libraries:kotlin:atomic" to "minekot-kt-atomic",
    ":libraries:kotlin:testing" to "minekot-kt-testing",
    ":libraries:codegen:core" to "minekot-codegen-core",
    ":libraries:codegen:ksp" to "minekot-ksp-helpers",
    ":libraries:adventure:common" to "minekot-adv-common",
    ":libraries:adventure:ansi" to "minekot-adv-ansi",
    ":libraries:adventure:json" to "minekot-adv-json",
    ":libraries:adventure:minimessage" to "minekot-adv-minimessage",
).forEach { (project, name) ->
    include(project)
    project(project).name = name
}
