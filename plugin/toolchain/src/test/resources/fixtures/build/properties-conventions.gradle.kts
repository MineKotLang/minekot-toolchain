plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotPropertyConventions") {
    doLast {
        val extension = project.extensions.getByType(org.minekot.toolchain.MineKotToolchainExtension::class.java)
        val javaExtension = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
        project.configurations.getByName("implementation").dependencies.forEach {
            println("${it.group}:${it.name}:${it.version}")
        }
        project.repositories.withType(org.gradle.api.artifacts.repositories.MavenArtifactRepository::class.java)
            .forEach {
                println("repository=${it.name}:${it.url}")
            }
        println("toolchain=${javaExtension.toolchain.languageVersion.get().asInt()}")
        println("serialization=${extension.serialization.enabled.get()}")
        println("shadow=${extension.shadow.enabled.get()}")
    }
}
