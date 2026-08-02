plugins {
    id("org.minekot.toolchain")
}

val projectJavaVersion = 21
val smokeProjectDescription = "MineKot Java ${projectJavaVersion} smoke"

description = smokeProjectDescription

listOf(
    "minekot.rootDir",
    rootProject.projectDir.absolutePath,
)
    .forEach { dependency ->
        require(dependency.isNotBlank())
    }

configurations.configureEach {
    resolutionStrategy {
        cacheDynamicVersionsFor(
            0,
            "seconds",
        )
        cacheChangingModulesFor(
            0,
            "seconds",
        )
    }
}

minekotToolchain {
    build {
        javaVersion = projectJavaVersion
    }
    adventure {
        enabled = false
    }
    lint {
        enabled = true
        autoCorrect = false
        buildUponDefaultConfig = false
        configFile.set(layout.projectDirectory.file("config/detekt/minekot.yml"))
    }
    testing {
        enabled = false
    }
}
