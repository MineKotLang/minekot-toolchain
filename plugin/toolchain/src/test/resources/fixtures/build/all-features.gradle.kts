plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    serialization {
        libraryVersion.set("1.1.1-minekot-test")
    }
    io {
        libraryVersion.set("2.2.2-minekot-test")
    }
    coroutines {
        libraryVersion.set("3.3.3-minekot-test")
    }
    atomic {
        libraryVersion.set("4.4.4-minekot-test")
    }
    codegen {
        enabled.set(true)
    }
    adventure {
        libraryVersion.set("5.5.5-minekot-test")
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotFeatureDependencies") {
    doLast {
        project.configurations.getByName("implementation").dependencies.forEach {
            println("${it.group}:${it.name}:${it.version}")
        }
        project.configurations.getByName("testImplementation").dependencies.forEach {
            println("${it.group}:${it.name}:${it.version}")
        }
    }
}
