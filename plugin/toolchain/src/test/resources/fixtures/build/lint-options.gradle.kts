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
        val detekt = project.extensions.getByType(io.gitlab.arturbosch.detekt.extensions.DetektExtension::class.java)
        println("autoCorrect=${detekt.autoCorrect}")
        println("buildUponDefaultConfig=${detekt.buildUponDefaultConfig}")
        detekt.config.files.forEach {
            println("config=${it.name}")
        }
    }
}
