import io.github.klahap.dotenv.DotEnvBuilder
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dotenv)
}

val env = DotEnvBuilder.dotEnv {
    addSystemEnv()
    addFileIfExists(file(".env"))
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

val projectJavaVersion = 21
val artifactIds = mapOf(
    ":plugin:minekot-toolchain-gradle-plugin" to "minekot-toolchain-gradle-plugin",
    ":plugin:minekot-toolchain-lint-rules" to "minekot-toolchain-lint-rules",
    ":libraries:kotlin:minekot-kt-common" to "minekot-kt-common",
    ":libraries:kotlin:minekot-kt-reflection" to "minekot-kt-reflection",
    ":libraries:kotlin:minekot-kt-serialization" to "minekot-kt-serialization",
    ":libraries:kotlin:minekot-kt-io" to "minekot-kt-io",
    ":libraries:kotlin:minekot-kt-coroutines" to "minekot-kt-coroutines",
    ":libraries:kotlin:minekot-kt-atomic" to "minekot-kt-atomic",
    ":libraries:kotlin:minekot-kt-testing" to "minekot-kt-testing",
    ":libraries:codegen:minekot-codegen-core" to "minekot-codegen-core",
    ":libraries:codegen:minekot-ksp" to "minekot-ksp",
    ":libraries:adventure:minekot-adv-common" to "minekot-adv-common",
    ":libraries:adventure:minekot-adv-ansi" to "minekot-adv-ansi",
    ":libraries:adventure:minekot-adv-json" to "minekot-adv-json",
    ":libraries:adventure:minekot-adv-minimessage" to "minekot-adv-minimessage",
)

tasks.register<Exec>("mineKotSmokeTest") {
    group = "verification"
    description = "Runs the standalone MineKot plugin smoke project."
    workingDir = layout.projectDirectory.dir("samples/smoke").asFile
    commandLine(layout.projectDirectory.file("gradlew").asFile.absolutePath, "mineKotSmokeTest", "--no-daemon")
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    pluginManager.withPlugin("base") {
        extensions.configure<BasePluginExtension> {
            archivesName.set(artifactIds[path] ?: name)
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension> {
            withSourcesJar()
        }

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(projectJavaVersion)
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
                allWarningsAsErrors.set(false)
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            enabled = false
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        dependencies.add("testImplementation", libs.junit.jupiter)
        dependencies.add("testRuntimeOnly", libs.junit.platform.launcher)

        tasks.named<ProcessResources>("processResources") {
            from(rootProject.layout.projectDirectory.file("LICENSE"))
            from(rootProject.layout.projectDirectory.file("NOTICE"))
        }
    }

    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            if ((plugins.hasPlugin("java") || plugins.hasPlugin("java-library")) && !plugins.hasPlugin("java-gradle-plugin")) {
                publications.create("mavenJava", MavenPublication::class.java) {
                    from(components["java"])
                    artifactId = artifactIds[path] ?: project.name
                }
            }

            repositories {
                maven {
                    name = "minekot"
                    url = uri(
                        if (version.toString().endsWith("SNAPSHOT")) {
                            "https://maven.minekot.org/snapshots"
                        } else {
                            "https://maven.minekot.org/releases"
                        },
                    )
                    credentials {
                        username = env["MINEKOT_MAVEN_USERNAME"]
                        password = env["MINEKOT_MAVEN_PASSWORD"]
                    }
                }
            }

            publications.withType<MavenPublication>().configureEach {
                pom {
                    name.set(project.name)
                    description.set("MineKot toolchain artifact ${project.path}")
                    url.set("https://github.com/MineKotLang/minekot-toolchain")
                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    developers {
                        developer {
                            id.set("minekot")
                            name.set("MineKot Team")
                        }
                    }
                }
            }
        }

        tasks.withType<PublishToMavenRepository>().configureEach {
            doFirst {
                if (repository.name == "minekot") {
                    val hasCredentials = env["MINEKOT_MAVEN_USERNAME"] != null && env["MINEKOT_MAVEN_PASSWORD"] != null
                    require(hasCredentials) {
                        "MineKot Maven publish requires MINEKOT_MAVEN_USERNAME and MINEKOT_MAVEN_PASSWORD."
                    }
                }
            }
        }
    }
}
