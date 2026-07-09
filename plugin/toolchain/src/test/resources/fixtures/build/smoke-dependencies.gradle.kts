plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotSmokeDependencies") {
    doLast {
        val smoke = project.tasks.named("mineKotSmokeTest").get()
        smoke.taskDependencies.getDependencies(smoke).forEach {
            println("dependency=${it.name}")
        }
    }
}
