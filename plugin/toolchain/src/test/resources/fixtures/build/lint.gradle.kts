plugins {
    id("org.minekot.toolchain") version "+"
}

tasks.register("printMineKotLint") {
    doLast {
        println("detekt=${project.plugins.hasPlugin("dev.detekt")}")
        val detekt = project.extensions.getByType(dev.detekt.gradle.extensions.DetektExtension::class.java)
        println("buildUponDefaultConfig=${detekt.buildUponDefaultConfig.get()}")
        project.configurations.getByName("detektPlugins").dependencies.forEach {
            println("${it.group}:${it.name}:${it.version}")
        }
        project.configurations.getByName("detektPlugins").files.forEach {
            println("detektPluginFile=${it.name}")
        }
    }
}
