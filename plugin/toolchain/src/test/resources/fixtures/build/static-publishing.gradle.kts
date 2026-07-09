plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    publishing {
        minekotRepository.set(false)
        staticRepositoryDirectory.set(layout.buildDirectory.dir("static-repo"))
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotStaticPublishing") {
    doLast {
        val publishing = project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
        publishing.repositories.forEach {
            println("repository=${it.name}")
        }
    }
}
