plugins {
    id("org.minekot.toolchain")
}

minekotToolchain {
    lint {
        enabled.set(false)
    }
    publishing {
        enabled.set(false)
    }
    ciCd {
        enabled.set(true)
        supportedVersions.set(listOf("1.21.8", "1.21.11"))
        publicationProjects.set(emptyList())
        expectedArtifacts.set(listOf("paper.jar", "velocity.jar"))
        requiredEvidenceItems.set(listOf("fabric-1.21.8", "neoforge-1.21.8"))
    }
}
