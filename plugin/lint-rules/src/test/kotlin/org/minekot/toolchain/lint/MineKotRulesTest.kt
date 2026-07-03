package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineKotRulesTest {
    @Test
    fun `rule descriptors are complete`() {
        mineKotRuleDescriptors.forEach { descriptor ->
            assertTrue(descriptor.id.isNotBlank())
            assertTrue(descriptor.codestyleSection.isNotBlank())
            assertTrue(descriptor.failingFixture.endsWith(".kt"))
            assertTrue(descriptor.cleanFixture.endsWith(".kt"))
        }
    }

    @Test
    fun `forbidden try catch reports catch clauses`() {
        val findings = ForbiddenTryCatchRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun run() {
                try {
                    error("boom")
                } catch (exception: RuntimeException) {
                    println(exception.message)
                }
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `string template rule reports simple template entries`() {
        val findings = StringTemplateBracesRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun greeting(name: String): String = "Hello ${'$'}name"
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `minimessage rule reports legacy color codes`() {
        val findings = MiniMessageTextRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun message(): String = "&aMineKot"
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `minimessage rule reports string concatenation`() {
        val findings = MiniMessageTextRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun message(name: String): String = "Hello, " + name
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `minimessage rule accepts minimessage tags`() {
        val findings = MiniMessageTextRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun message(): String = "<green>MineKot</green>"
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `minimessage rule ignores non string addition with quoted comment`() {
        val findings = MiniMessageTextRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun sum(a: Int, b: Int): Int = a + b // "not text"
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `missing kdoc rule reports public declarations`() {
        val findings = MissingKDocRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            class Service {
                fun run(): String = "MineKot"
                val name: String = "MineKot"
            }
            """.trimIndent(),
        )

        assertEquals(3, findings.size)
    }

    @Test
    fun `missing kdoc rule accepts documented declarations`() {
        val findings = MissingKDocRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            /**
             * Service.
             */
            class Service {
                /**
                 * Runs service.
                 */
                fun run(): String = "MineKot"

                /**
                 * Service name.
                 */
                val name: String = "MineKot"
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `missing kdoc rule ignores private internal and local declarations`() {
        val findings = MissingKDocRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            private class PrivateService

            internal class InternalService

            class PublicService {
                private fun privateRun(): String = "MineKot"

                internal fun internalRun(): String = "MineKot"

                fun publicRun(): String {
                    fun localRun(): String = "MineKot"
                    return localRun()
                }
            }
            """.trimIndent(),
        )

        assertEquals(2, findings.size)
    }

    @Test
    fun `missing kdoc rule reports public constructor properties`() {
        val findings = MissingKDocRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            /**
             * Service.
             */
            class Service(val name: String)
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `missing kdoc rule ignores public members inside non public containers`() {
        val findings = MissingKDocRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            private class PrivateService {
                fun run(): String = "MineKot"
                val name: String = "MineKot"
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `missing kdoc rule ignores local classes`() {
        val findings = MissingKDocRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            /**
             * Creates service.
             */
            fun create() {
                class LocalService {
                    fun run(): String = "MineKot"
                }
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `coroutine preference rule reports thread sleep`() {
        val findings = CoroutinePreferenceRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun run() {
                Thread.sleep(250)
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `coroutine preference rule reports thread construction`() {
        val findings = CoroutinePreferenceRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun run() {
                Thread {
                    println("MineKot")
                }
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `coroutine preference rule reports fully qualified thread APIs`() {
        val findings = CoroutinePreferenceRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun run() {
                java.lang.Thread.sleep(250)
                java.util.Timer()
            }
            """.trimIndent(),
        )

        assertEquals(2, findings.size)
    }

    @Test
    fun `magic number rule reports non-trivial numeric literals`() {
        val findings = MagicNumberRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun retryLimit(): Int = 7
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `magic number rule accepts constants and common numbers`() {
        val findings = MagicNumberRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            private const val RETRY_LIMIT = 7

            fun index(): Int = 0
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `magic number rule reports suffixed underscored and radix numbers`() {
        val findings = MagicNumberRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun values(): List<Number> = listOf(100L, 1_000, 0xff, 10.0f)
            """.trimIndent(),
        )

        assertEquals(4, findings.size)
    }

    @Test
    fun `result handling rule reports unsafe get or throw`() {
        val findings = ResultHandlingRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun value(): String = runCatching { "MineKot" }.getOrThrow()
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `result handling rule accepts explicit handling`() {
        val findings = ResultHandlingRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun value(): String = runCatching { "MineKot" }.getOrElse { "fallback" }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `result handling rule accepts run catching returned from try expression`() {
        val findings = ResultHandlingRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun value(): Result<String> = try {
                runCatching { "MineKot" }
            } finally {
                println("done")
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `result handling rule reports ignored branch without braces`() {
        val findings = ResultHandlingRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun value(enabled: Boolean) {
                if (enabled) runCatching { "MineKot" }
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `result handling rule accepts run catching returned from lambda`() {
        val findings = ResultHandlingRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            fun values(): List<Result<String>> = listOf("MineKot").map { value ->
                runCatching { value }
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `kotlinx preference rule reports blocked java imports`() {
        val findings = KotlinxPreferenceRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            import java.io.File

            fun path(file: File): String = file.path
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `kotlinx preference rule accepts kotlin path imports`() {
        val findings = KotlinxPreferenceRule(io.gitlab.arturbosch.detekt.api.Config.empty).lint(
            """
            import java.nio.file.Path

            fun path(path: Path): String = path.toString()
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }
}
