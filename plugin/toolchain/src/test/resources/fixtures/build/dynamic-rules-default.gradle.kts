plugins {
    id("org.minekot.toolchain")
}

tasks.register("printDynamicRulesDefault") {
    doLast {
        val extension = project.extensions.getByType<org.minekot.toolchain.MineKotToolchainExtension>()
        println("rulesEnabled=${extension.lint.rules.enabled.get()}")
        println("rulesVersion=${org.minekot.toolchain.RulesLockBlock.DEFAULT_VERSION}")
        println("rulesDigest=${org.minekot.toolchain.RulesLockBlock.DEFAULT_MANIFEST_SHA256}")
        println("resolveTask=${tasks.findByName("resolveMineKotRules") != null}")
    }
}
