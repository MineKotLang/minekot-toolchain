plugins {
    id("org.minekot.toolchain")
}

minekotToolchain {
    lint {
        rules.lock("1.0.7", "0".repeat(64))
    }
}

tasks.register("printDynamicRulesLock") {
    doLast {
        val extension = project.extensions.getByType<org.minekot.toolchain.MineKotToolchainExtension>()
        println("rulesVersion=${extension.lint.rules.version.get()}")
        println("rulesDigest=${extension.lint.rules.manifestSha256.get()}")
        println("resolveTask=${tasks.findByName("resolveMineKotRules") != null}")
    }
}
