plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    adventure {
        enabled.set(false)
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotDependencies") {
    doLast {
        configurations.getByName("implementation").dependencies.forEach {
            println("${it.group}:${it.name}:${it.version}")
        }
    }
}
