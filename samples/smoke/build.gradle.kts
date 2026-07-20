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
        javaVersion.set(projectJavaVersion)
    }
    adventure {
        enabled.set(false)
    }
    lint {
        enabled.set(true)
        autoCorrect.set(false)
        buildUponDefaultConfig.set(false)
        configFile.set(layout.projectDirectory.file("config/detekt/minekot.yml"))
    }
    testing {
        enabled.set(false)
    }
}
