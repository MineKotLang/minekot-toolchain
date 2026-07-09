plugins {
    id("org.minekot.toolchain") version "+"
}

minekotToolchain {
    lint {
        enabled.set(false)
    }
}

tasks.register("printMineKotConventions") {
    doLast {
        val javaExtension = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
        val testTask = project.tasks.named("test").get() as org.gradle.api.tasks.testing.Test
        println("toolchain=${javaExtension.toolchain.languageVersion.get().asInt()}")
        println("sourcesJar=${project.tasks.names.contains("sourcesJar")}")
        println("javadocJar=${project.tasks.names.contains("javadocJar")}")
        println("junit=${testTask.options.javaClass.simpleName}")
        println("codestyle=${project.tasks.names.contains("writeMineKotCodestyle")}")
        println("projectFiles=${project.tasks.names.contains("writeMineKotProjectFiles")}")
        println("smoke=${project.tasks.names.contains("mineKotSmokeTest")}")
    }
}
