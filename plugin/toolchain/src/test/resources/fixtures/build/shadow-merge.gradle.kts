plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    shadow {
        enabled.set(true)
        mergeServiceFiles.set(false)
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotShadowMerge") {
    doLast {
        val shadowJar =
            project.tasks.named("shadowJar").get() as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
        println("transformers=${shadowJar.transformers.get().size}")
    }
}
