plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    build {
        javaVersion.set(17)
        allWarningsAsErrors.set(true)
        contextParameters.set(false)
    }
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotBuildOptions") {
    doLast {
        val extension = project.extensions.getByType(org.minekot.toolchain.MineKotToolchainExtension::class.java)
        val javaExtension = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
        val kotlinCompile =
            project.tasks.named("compileKotlin").get() as org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        println("toolchain=${javaExtension.toolchain.languageVersion.get().asInt()}")
        println("jvmTarget=${kotlinCompile.compilerOptions.jvmTarget.get().target}")
        println("allWarningsAsErrors=${kotlinCompile.compilerOptions.allWarningsAsErrors.get()}")
        println("contextParameters=${extension.build.contextParameters.get()}")
    }
}
