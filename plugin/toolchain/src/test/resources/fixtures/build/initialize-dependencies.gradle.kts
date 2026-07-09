plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotInitializeDependencies") {
    doLast {
        val initialize = project.tasks.named("mineKotInitializeProject").get()
        initialize.taskDependencies.getDependencies(initialize).forEach {
            println("dependency=${it.name}")
        }
    }
}
