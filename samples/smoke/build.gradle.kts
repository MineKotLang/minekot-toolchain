plugins {
    id("org.minekot.toolchain")
}

configurations.configureEach {
    resolutionStrategy {
        cacheDynamicVersionsFor(0, "seconds")
        cacheChangingModulesFor(0, "seconds")
    }
}

minekotToolchain {
    adventure {
        enabled.set(false)
    }
    lint {
        enabled.set(false)
    }
    testing {
        enabled.set(false)
    }
}
