plugins {
    id("org.minekot.toolchain") version "+"
}

tasks.register("printMineKotLint") {
    doLast {
        println("detekt=${project.plugins.hasPlugin("io.gitlab.arturbosch.detekt")}")
        project.configurations.getByName("detektPlugins").dependencies.forEach {
            println("${it.group}:${it.name}:${it.version}")
        }
    }
}
