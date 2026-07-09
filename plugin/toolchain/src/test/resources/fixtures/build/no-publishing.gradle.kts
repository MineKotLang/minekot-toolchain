plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    publishing {
        enabled.set(false)
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotNoPublishing") {
    doLast {
        println("mavenPublish=${project.plugins.hasPlugin("maven-publish")}")
    }
}
