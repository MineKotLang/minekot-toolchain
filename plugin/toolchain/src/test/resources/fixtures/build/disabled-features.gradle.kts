plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    reflection {
        enabled.set(false)
    }
    serialization {
        enabled.set(false)
    }
    io {
        enabled.set(false)
    }
    coroutines {
        enabled.set(false)
    }
    atomic {
        enabled.set(false)
    }
    testing {
        enabled.set(false)
    }
    adventure {
        enabled.set(false)
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotDisabledFeatures") {
    doLast {
        println("serializationPlugin=${project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.serialization")}")
        println("detekt=${project.plugins.hasPlugin("dev.detekt")}")
        project.configurations.getByName("implementation").dependencies.forEach {
            println("${it.group}:${it.name}:${it.version}")
        }
        project.configurations.getByName("testImplementation").dependencies.forEach {
            println("${it.group}:${it.name}:${it.version}")
        }
    }
}
