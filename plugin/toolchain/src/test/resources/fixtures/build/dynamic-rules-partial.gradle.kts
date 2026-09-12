plugins {
    id("org.minekot.toolchain")
}

minekotToolchain {
    lint {
        rules.enabled = true
        rules.version = "1.0.7"
    }
}
