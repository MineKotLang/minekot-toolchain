plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    shadow {
        enabled.set(true)
        classifier.set("bundle")
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotShadow") {
    doLast {
        val shadowJar =
            project.tasks.named("shadowJar").get() as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
        println("shadow=${project.plugins.hasPlugin("com.gradleup.shadow")}")
        println("classifier=${shadowJar.archiveClassifier.get()}")
        println("duplicates=${shadowJar.duplicatesStrategy}")
    }
}
