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

include(
    ":plugin:toolchain",
    ":plugin:lint-rules",
    ":libraries:kotlin:common",
    ":libraries:kotlin:reflection",
    ":libraries:kotlin:serialization",
    ":libraries:kotlin:io",
    ":libraries:kotlin:coroutines",
    ":libraries:kotlin:atomic",
    ":libraries:kotlin:testing",
    ":libraries:codegen:core",
    ":libraries:codegen:ksp",
    ":libraries:adventure:common",
    ":libraries:adventure:ansi",
    ":libraries:adventure:json",
    ":libraries:adventure:minimessage",
)

project(":plugin:toolchain").name = "minekot-toolchain-gradle-plugin"
project(":plugin:lint-rules").name = "minekot-toolchain-lint-rules"
project(":libraries:kotlin:common").name = "minekot-kt-common"
project(":libraries:kotlin:reflection").name = "minekot-kt-reflection"
project(":libraries:kotlin:serialization").name = "minekot-kt-serialization"
project(":libraries:kotlin:io").name = "minekot-kt-io"
project(":libraries:kotlin:coroutines").name = "minekot-kt-coroutines"
project(":libraries:kotlin:atomic").name = "minekot-kt-atomic"
project(":libraries:kotlin:testing").name = "minekot-kt-testing"
project(":libraries:codegen:core").name = "minekot-codegen-core"
project(":libraries:codegen:ksp").name = "minekot-ksp"
project(":libraries:adventure:common").name = "minekot-adv-common"
project(":libraries:adventure:ansi").name = "minekot-adv-ansi"
project(":libraries:adventure:json").name = "minekot-adv-json"
project(":libraries:adventure:minimessage").name = "minekot-adv-minimessage"
