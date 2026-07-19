plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    lint {
        autoCorrect.set(true)
        buildUponDefaultConfig.set(false)
        configFile.set(layout.projectDirectory.file("minekot-detekt.yml"))
    }
}

tasks.register("printMineKotLintOptions") {
    doLast {
        val detekt = project.extensions.getByType(dev.detekt.gradle.extensions.DetektExtension::class.java)
        println("autoCorrect=${detekt.autoCorrect.get()}")
        println("buildUponDefaultConfig=${detekt.buildUponDefaultConfig.get()}")
        detekt.config.files.forEach {
            println("config=${it.name}")
        }
    }
}
