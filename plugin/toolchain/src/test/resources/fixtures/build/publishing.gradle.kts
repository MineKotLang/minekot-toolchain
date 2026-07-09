plugins {
    id("org.minekot.toolchain") version "+"
}

version = "1.2.3-SNAPSHOT"

minekotToolchain {
    publishing {
        staticRepositoryDirectory.set(layout.buildDirectory.dir("static-repo"))
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotPublishing") {
    doLast {
        val publishing = project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
        println("mavenPublish=${project.plugins.hasPlugin("maven-publish")}")
        publishing.publications.forEach {
            println("publication=${it.name}")
        }
        publishing.repositories.forEach {
            println("repository=${it.name}")
        }
    }
}
