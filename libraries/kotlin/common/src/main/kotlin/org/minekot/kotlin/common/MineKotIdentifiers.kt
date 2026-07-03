package org.minekot.kotlin.common

/**
 * Stable typed MineKot identifier.
 *
 * @property value Identifier value.
 */
@JvmInline
value class MineKotId(val value: String) {
    init {
        value.requireMineKotName("id")
    }
}

/**
 * Namespaced MineKot key.
 *
 * @property value Key value.
 */
@JvmInline
value class MineKotKey private constructor(val value: String) : Comparable<MineKotKey> {
    /**
     * Key namespace.
     */
    val namespace: String
        get() = value.substringBefore(":")

    /**
     * Key path.
     */
    val path: String
        get() = value.substringAfter(":")

    override fun compareTo(other: MineKotKey): Int =
        value.compareTo(other.value)

    override fun toString(): String =
        value

    companion object {
        /**
         * Creates a key from a string.
         *
         * @param value Raw key value.
         * @return Valid MineKot key.
         */
        operator fun invoke(value: String): MineKotKey {
            val normalized = value.lowercase()
            normalized.requireMineKotKey("key")
            return MineKotKey(normalized)
        }

        /**
         * Creates a key from namespace and path.
         *
         * @param namespace Key namespace.
         * @param path Key path.
         * @return Valid MineKot key.
         */
        fun of(namespace: String, path: String): MineKotKey =
            invoke("${namespace}:${path}")

        /**
         * Parses a key or returns null.
         *
         * @param value Raw key value.
         * @return Parsed key, or null.
         */
        fun parseOrNull(value: String): MineKotKey? =
            runCatching {
                invoke(value)
            }.getOrNull()

        /**
         * Parses a key into a [Result].
         *
         * @param value Raw key value.
         * @return Parsed key result.
         */
        fun parseResult(value: String): Result<MineKotKey> =
            runCatching {
                invoke(value)
            }
    }
}
