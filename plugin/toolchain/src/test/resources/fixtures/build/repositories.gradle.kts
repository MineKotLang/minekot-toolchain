plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    repositories {
        mavenCentral.set(false)
        mavenLocal.set(false)
        minekotReleases.set(true)
        minekotSnapshots.set(false)
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotRepositories") {
    doLast {
        project.repositories.withType(org.gradle.api.artifacts.repositories.MavenArtifactRepository::class.java)
            .forEach {
                println("${it.name}:${it.url}")
            }
    }
}
