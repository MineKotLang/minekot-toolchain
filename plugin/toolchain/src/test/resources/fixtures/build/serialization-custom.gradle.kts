plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    serialization {
        libraryVersion.set("9.9.9-minekot-test")
    }
    adventure {
        enabled.set(false)
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotSerialization") {
    doLast {
        println("serializationPlugin=${project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.serialization")}")
        project.configurations.getByName("implementation").dependencies.forEach {
            println("${it.group}:${it.name}:${it.version}")
        }
    }
}
