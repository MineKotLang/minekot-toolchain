package org.minekot.toolchain

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

/** Versioned assisted-fix request document. */
@Serializable
internal data class MineKotAssistRequestDocument(
    val schemaVersion: Int = 1,
    val confirmation: MineKotAssistConfirmation? = null,
    val requests: List<MineKotAssistRequest> = emptyList(),
)

/** Explicit application confirmation for one preview plan. */
@Serializable
internal data class MineKotAssistConfirmation(
    val planId: String,
    val confirmed: Boolean,
)

/** One requested assisted transformation. */
@Serializable
internal data class MineKotAssistRequest(
    val findingId: String,
    val action: String,
    val options: MineKotAssistOptions? = null,
)

/** Typed options for one assisted transformation. */
@Serializable(with = MineKotAssistOptionsSerializer::class)
internal sealed interface MineKotAssistOptions {
    /** Converts options to stable validation keys. */
    fun values(): Map<String, String?>
}

/** File-to-Path migration options. */
@Serializable
internal data class MineKotFileToPathOptions(val symbol: String? = null) : MineKotAssistOptions {
    override fun values(): Map<String, String?> = mapOf("symbol" to symbol)
}

/** Magic-number extraction options. */
@Serializable
internal data class MineKotMagicNumberOptions(
    @kotlinx.serialization.SerialName("constant-name") val constantName: String? = null,
) : MineKotAssistOptions {
    override fun values(): Map<String, String?> = mapOf("constant-name" to constantName)
}

/** Result-handling options. */
@Serializable
internal data class MineKotResultHandlingOptions(
    @kotlinx.serialization.SerialName("result-mode") val resultMode: String? = null,
    @kotlinx.serialization.SerialName("fallback-expression") val fallbackExpression: String? = null,
    @kotlinx.serialization.SerialName("failure-handler") val failureHandler: String? = null,
) : MineKotAssistOptions {
    override fun values(): Map<String, String?> = mapOf(
        "result-mode" to resultMode,
        "fallback-expression" to fallbackExpression,
        "failure-handler" to failureHandler,
    )
}

/** Coroutine lifecycle migration options. */
@Serializable
internal data class MineKotCoroutineOptions(
    @kotlinx.serialization.SerialName("scope-property") val scopeProperty: String? = null,
    val dispatcher: String? = null,
    @kotlinx.serialization.SerialName("cancellation-point") val cancellationPoint: String? = null,
    @kotlinx.serialization.SerialName("cleanup-method") val cleanupMethod: String? = null,
) : MineKotAssistOptions {
    override fun values(): Map<String, String?> = mapOf(
        "scope-property" to scopeProperty,
        "dispatcher" to dispatcher,
        "cancellation-point" to cancellationPoint,
        "cleanup-method" to cleanupMethod,
    )
}

/** MiniMessage migration options. */
@Serializable
internal data class MineKotMiniMessageOptions(
    @kotlinx.serialization.SerialName("localization-mode") val localizationMode: String? = null,
    @kotlinx.serialization.SerialName("semantic-tag") val semanticTag: String? = null,
    @kotlinx.serialization.SerialName("localization-expression") val localizationExpression: String? = null,
    @kotlinx.serialization.SerialName("placeholders-expression") val placeholdersExpression: String? = null,
) : MineKotAssistOptions {
    override fun values(): Map<String, String?> = mapOf(
        "localization-mode" to localizationMode,
        "semantic-tag" to semanticTag,
        "localization-expression" to localizationExpression,
        "placeholders-expression" to placeholdersExpression,
    )
}

/** Try/catch conversion options. */
@Serializable
internal data class MineKotTryCatchOptions(
    @kotlinx.serialization.SerialName("failure-mode") val failureMode: String? = null,
) : MineKotAssistOptions {
    override fun values(): Map<String, String?> = mapOf("failure-mode" to failureMode)
}

/** Selects typed options from each action-specific key set without adding a wire discriminator. */
internal object MineKotAssistOptionsSerializer : JsonContentPolymorphicSerializer<MineKotAssistOptions>(
    MineKotAssistOptions::class,
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<MineKotAssistOptions> {
        val keys = element.jsonObject.keys
        return when {
            "symbol" in keys -> MineKotFileToPathOptions.serializer()
            "constant-name" in keys -> MineKotMagicNumberOptions.serializer()
            "result-mode" in keys -> MineKotResultHandlingOptions.serializer()
            "scope-property" in keys -> MineKotCoroutineOptions.serializer()
            "localization-mode" in keys -> MineKotMiniMessageOptions.serializer()
            "failure-mode" in keys -> MineKotTryCatchOptions.serializer()
            else -> throw IllegalArgumentException("Assisted-fix options do not match a supported action.")
        }
    }
}

/** Machine-readable assisted-fix preview. */
@Serializable
internal data class MineKotAssistReport(
    val schemaVersion: Int,
    val planId: String,
    val candidates: List<MineKotAssistCandidate>,
    val changedFiles: List<String>,
)

/** One stable assisted-fix candidate. */
@Serializable
internal data class MineKotAssistCandidate(
    val findingId: String,
    val action: String,
    val path: String,
    val line: Int,
    val status: String,
    val requiredOptions: List<String>,
    val message: String,
)
