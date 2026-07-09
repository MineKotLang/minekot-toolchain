package org.minekot.toolchain

/**
 * Describes a bundled file that can be written to a consumer project.
 *
 * @property resourcePath Classpath resource path.
 * @property outputPath Consumer project path.
 */
internal data class MineKotResourceDescriptor(
    val resourcePath: String,
    val outputPath: String,
)

/**
 * Describes a codestyle file produced by the toolchain.
 *
 * @property resourcePath Classpath resource path.
 * @property outputPath Consumer project path.
 */
internal data class MineKotCodestyleDescriptor(
    val resourcePath: String,
    val outputPath: String,
)

/**
 * Describes a MineKot published library module.
 *
 * @property path Gradle project path.
 * @property artifactId Published artifact id.
 */
internal data class MineKotLibraryModuleDescriptor(
    val path: String,
    val artifactId: String,
)

internal val mineKotCodestyleDescriptors: List<MineKotCodestyleDescriptor> = listOf(
    MineKotCodestyleDescriptor("codestyle/detekt.yml", "config/detekt/minekot.yml"),
    MineKotCodestyleDescriptor("codestyle/intellij-code-style.xml", ".idea/codeStyles/MineKot.xml"),
)

internal val mineKotLibraryModuleDescriptors: List<MineKotLibraryModuleDescriptor> = listOf(
    MineKotLibraryModuleDescriptor(":plugin:minekot-toolchain-gradle-plugin", "minekot-toolchain-gradle-plugin"),
    MineKotLibraryModuleDescriptor(":plugin:minekot-toolchain-lint-rules", "minekot-toolchain-lint-rules"),
    MineKotLibraryModuleDescriptor(":libraries:kotlin:minekot-kt-common", "minekot-kt-common"),
    MineKotLibraryModuleDescriptor(":libraries:kotlin:minekot-kt-reflection", "minekot-kt-reflection"),
    MineKotLibraryModuleDescriptor(":libraries:kotlin:minekot-kt-serialization", "minekot-kt-serialization"),
    MineKotLibraryModuleDescriptor(":libraries:kotlin:minekot-kt-io", "minekot-kt-io"),
    MineKotLibraryModuleDescriptor(":libraries:kotlin:minekot-kt-coroutines", "minekot-kt-coroutines"),
    MineKotLibraryModuleDescriptor(":libraries:kotlin:minekot-kt-atomic", "minekot-kt-atomic"),
    MineKotLibraryModuleDescriptor(":libraries:kotlin:minekot-kt-testing", "minekot-kt-testing"),
    MineKotLibraryModuleDescriptor(":libraries:codegen:minekot-codegen-core", "minekot-codegen-core"),
    MineKotLibraryModuleDescriptor(":libraries:codegen:minekot-ksp", "minekot-ksp"),
    MineKotLibraryModuleDescriptor(":libraries:adventure:minekot-adv-common", "minekot-adv-common"),
    MineKotLibraryModuleDescriptor(":libraries:adventure:minekot-adv-ansi", "minekot-adv-ansi"),
    MineKotLibraryModuleDescriptor(":libraries:adventure:minekot-adv-json", "minekot-adv-json"),
    MineKotLibraryModuleDescriptor(":libraries:adventure:minekot-adv-minimessage", "minekot-adv-minimessage"),
)
