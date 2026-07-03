plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}


group = project.findProperty("group")?.toString() ?: missingProperty("group")
version = project.findProperty("version")?.toString() ?: missingProperty("version")
val projectJavaVersion = 21

kotlin.jvmToolchain(projectJavaVersion)

repositories {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
}

dependencies {
    implementation(libs.bundles.kotlin.core)
    implementation(libs.bundles.adventure)
    testImplementation(libs.bundles.testing)
}

configure<JavaPluginExtension> {
    withSourcesJar()
}

tasks {
    jar {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("${project.name}-${project.version}.jar")
        destinationDirectory.set(file("${rootDir}/final"))

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        mergeServiceFiles()
    }

    processResources {
        from("${rootDir}") { include("NOTICE", "LICENSE") }
    }

    publishToMavenLocal {
        dependsOn("sourcesJar")
    }

    test {
        useJUnitPlatform()
    }

    build {
        dependsOn("publishToMavenLocal")
    }
}

private fun missingProperty(name: String): Nothing =
    throw IllegalStateException("Property '$name' is missing. Please define it in gradle.properties.")
