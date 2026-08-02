package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class MineKotAnalysisRulesTest {
    @Test
    fun `semantic rules require full analysis`() {
        val rules: List<Rule> = listOf(
            CoroutinePreferenceRule(Config.empty),
            ExplicitScopeInNestedScopeRule(Config.empty),
            MiniMessageTextRule(Config.empty),
            ResolvedApiPreferenceRule(Config.empty),
        )

        rules.forEach { rule ->
            assertTrue(rule is RequiresAnalysisApi)
        }
    }

    @Test
    fun `resolved API preference distinguishes direct JDK calls from wrappers and same names`() {
        val violation = ResolvedApiPreferenceRule(Config.empty).lintWithAnalysis(
            "import java.io.File\nfun open(): File = File(\"input.txt\")",
        )
        val helper = ResolvedApiPreferenceRule(Config.empty).lintWithAnalysis(
            "package org.minekot.kotlin.io\nimport java.io.File\nfun open(): File = File(\"input.txt\")",
        )
        val sameName = ResolvedApiPreferenceRule(Config.empty).lintWithAnalysis(
            "class File(val value: String)\nfun open(): File = File(\"input.txt\")",
        )

        assertEquals(1, violation.size)
        assertEquals(0, helper.size)
        assertEquals(0, sameName.size)
    }

    @Test
    fun `coroutine preference resolves JDK concurrency and honors adapter boundaries`() {
        val source =
            """
            import java.util.Timer
            import java.util.concurrent.CompletableFuture
            import java.util.concurrent.Executors

            fun run() {
                Thread.sleep(1)
                Thread { Unit }
                Timer()
                Executors.newSingleThreadExecutor()
                CompletableFuture.supplyAsync { "MineKot" }
            }
            """.trimIndent()
        val adapterSource = "package org.minekot.platform.paper\n" + source

        assertEquals(5, CoroutinePreferenceRule(Config.empty).lintWithAnalysis(source).size)
        assertEquals(0, CoroutinePreferenceRule(Config.empty).lintWithAnalysis(adapterSource).size)
    }

    @Test
    fun `MiniMessage rule resolves Adventure flow and ignores same named custom APIs`() {
        val classpath = classpathRoots(Audience::class.java, Component::class.java, MiniMessage::class.java)
        val violation = MiniMessageTextRule(Config.empty).lintWithAnalysis(
            """
            import net.kyori.adventure.audience.Audience
            import net.kyori.adventure.text.Component

            fun send(audience: Audience) {
                audience.sendMessage(Component.text("§cMineKot"))
            }
            """.trimIndent(),
            jvmClasspathRoots = classpath,
        )
        val clean = MiniMessageTextRule(Config.empty).lintWithAnalysis(
            """
            import net.kyori.adventure.audience.Audience
            import net.kyori.adventure.text.minimessage.MiniMessage

            fun send(audience: Audience) {
                audience.sendMessage(MiniMessage.miniMessage().deserialize("<red>MineKot</red>"))
            }
            """.trimIndent(),
            jvmClasspathRoots = classpath,
        )
        val custom = MiniMessageTextRule(Config.empty).lintWithAnalysis(
            "class Sink { fun sendMessage(value: String) = Unit }\nfun send(sink: Sink) { sink.sendMessage(\"§cMineKot\") }",
        )

        assertEquals(1, violation.size)
        assertEquals(0, clean.size)
        assertEquals(0, custom.size)
    }

    @Test
    fun `explicit scope resolves outer members across nested lambdas`() {
        val violation =
            """
            class Engine {
                val name: String = "MineKot"
                fun start(): Unit = Unit

                fun run(): Unit {
                    listOf(1).forEach {
                        listOf(2).forEach {
                            println(name)
                            start()
                        }
                    }
                }
            }
            """.trimIndent()
        val clean = violation
            .replace("println(name)", "println(this@Engine.name)")
            .replace("start()", "this@Engine.start()")
            .replace("fun this@Engine.start()", "fun start()")

        assertEquals(2, ExplicitScopeInNestedScopeRule(Config.empty).lintWithAnalysis(violation).size)
        assertEquals(0, ExplicitScopeInNestedScopeRule(Config.empty).lintWithAnalysis(clean).size)
    }

    private fun classpathRoots(vararg classes: Class<*>): List<Path> =
        classes.map { type -> Path.of(type.protectionDomain.codeSource.location.toURI()) }.distinct()
}
