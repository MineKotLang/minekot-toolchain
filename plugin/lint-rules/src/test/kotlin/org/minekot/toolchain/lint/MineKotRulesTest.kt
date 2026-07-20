package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Rule
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.*

class MineKotRulesTest {
    @Test
    fun `rule descriptors and provider expose the same unique rules`() {
        val descriptorsById = mineKotRuleDescriptors.associateBy { descriptor -> descriptor.id }
        val provider = MineKotRuleSetProvider()
        val ruleSet = provider.instance()
        val providedRuleIds = ruleSet.rules.keys.map { ruleName -> ruleName.value }.toSet()

        assertEquals(mineKotRuleDescriptors.size, descriptorsById.size)
        assertEquals(RuleSetId("minekot"), provider.ruleSetId)
        assertEquals(descriptorsById.keys, providedRuleIds)
        mineKotRuleDescriptors.forEach { descriptor ->
            assertTrue(descriptor.id.isNotBlank())
            assertTrue(descriptor.codestyleSection.isNotBlank())
            assertEquals(descriptor.id, descriptor.factory(Config.empty).ruleName.value)
        }
    }

    @Test
    fun `rule set provider is discoverable through service loader`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java)
            .filter { provider -> provider.ruleSetId == RuleSetId("minekot") }

        assertEquals(1, providers.size)
    }

    @TestFactory
    fun `provider rules flag canonical violations and accept canonical clean sources`(): List<DynamicTest> {
        val rules = MineKotRuleSetProvider().instance().rules
        return providerRuleCases.flatMap { (ruleId, ruleCase) ->
            val factory = rules.entries.single { entry -> entry.key.value == ruleId }.value
            listOf(
                DynamicTest.dynamicTest("${ruleId} provider flags canonical violation") {
                    val findings = factory(Config.empty).lint(ruleCase.violatingSource, ruleCase.filename)
                    assertTrue(findings.isNotEmpty(), "${ruleId} should flag canonical violation")
                },
                DynamicTest.dynamicTest("${ruleId} provider accepts canonical clean source") {
                    val findings = factory(Config.empty).lint(ruleCase.cleanSource, ruleCase.filename)
                    assertEquals(0, findings.size, "${ruleId}: ${findings.joinToString { it.message }}")
                },
            )
        }
    }

    @TestFactory
    fun `configured ktlint rules flag violations and accept clean sources`(): List<DynamicTest> {
        val provider = ServiceLoader.load(RuleSetProvider::class.java)
            .single { candidate -> candidate.ruleSetId == RuleSetId("ktlint") }
        val rules = provider.instance().rules
        return ktlintRuleCases.flatMap { (ruleId, ruleCase) ->
            val factory = rules.entries.single { entry -> entry.key.value == ruleId }.value
            listOf(
                DynamicTest.dynamicTest("${ruleId} ktlint flags canonical violation") {
                    val findings = factory(Config.empty).lint(ruleCase.violatingSource)
                    assertTrue(findings.isNotEmpty(), "${ruleId} should flag canonical violation")
                },
                DynamicTest.dynamicTest("${ruleId} ktlint accepts canonical clean source") {
                    val findings = factory(Config.empty).lint(ruleCase.cleanSource)
                    assertEquals(0, findings.size, "${ruleId}: ${findings.joinToString { it.message }}")
                },
            )
        }
    }

    @TestFactory
    fun `forbidden try catch matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { ForbiddenTryCatchRule(Config.empty) },
        RuleCase(
            name = "flags a try with one catch",
            expectedFindings = 1,
            source =
                """
                fun run() {
                    try {
                        error("boom")
                    } catch (exception: RuntimeException) {
                        println(exception.message)
                    }
                }
                """,
        ),
        RuleCase(
            name = "flags each nested try expression",
            expectedFindings = 2,
            source =
                """
                fun run() {
                    try {
                        try {
                            error("boom")
                        } catch (exception: IllegalStateException) {
                            println(exception.message)
                        }
                    } catch (exception: RuntimeException) {
                        println(exception.message)
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts try finally without a catch",
            expectedFindings = 0,
            source =
                """
                fun run() {
                    try {
                        println("MineKot")
                    } finally {
                        println("done")
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts run catching",
            expectedFindings = 0,
            source = "fun run(): Result<Unit> = runCatching { println(\"MineKot\") }",
        ),
    )

    @TestFactory
    fun `string template braces matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { StringTemplateBracesRule(Config.empty) },
        RuleCase(
            name = "flags each simple template entry",
            expectedFindings = 2,
            source = "fun greeting(first: String, last: String): String = \"Hello ${'$'}first ${'$'}last\"",
        ),
        RuleCase(
            name = "accepts braced template entries",
            expectedFindings = 0,
            source = "fun greeting(name: String): String = \"Hello ${'$'}{name}\"",
        ),
        RuleCase(
            name = "accepts an escaped dollar sign",
            expectedFindings = 0,
            source = "fun price(): String = \"\\${'$'}100\"",
        ),
        RuleCase(
            name = "accepts plain strings",
            expectedFindings = 0,
            source = "fun greeting(): String = \"Hello MineKot\"",
        ),
    )

    @TestFactory
    fun `minimessage text matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { MiniMessageTextRule(Config.empty) },
        RuleCase(
            name = "flags ampersand legacy color codes",
            expectedFindings = 1,
            source = "fun message(): String = \"&aMineKot\"",
        ),
        RuleCase(
            name = "flags section sign legacy color codes",
            expectedFindings = 1,
            source = "fun message(): String = \"§cMineKot\"",
        ),
        RuleCase(
            name = "reports one finding for a concatenation chain",
            expectedFindings = 1,
            source = "fun message(name: String, count: Int): String = \"Hello, \" + name + count",
        ),
        RuleCase(
            name = "accepts minimessage tags",
            expectedFindings = 0,
            source = "fun message(): String = \"<green>MineKot</green>\"",
        ),
        RuleCase(
            name = "accepts braced string interpolation",
            expectedFindings = 0,
            source = "fun message(name: String): String = \"Hello, ${'$'}{name}\"",
        ),
        RuleCase(
            name = "accepts URL query parameters that resemble color codes",
            expectedFindings = 0,
            source = "fun url(): String = \"https://minekot.org/?a=1&b=2\"",
        ),
        RuleCase(
            name = "accepts numeric addition with a quoted comment",
            expectedFindings = 0,
            source = "fun sum(a: Int, b: Int): Int = a + b // \"not text\"",
        ),
        RuleCase(
            name = "accepts internal identifier concatenation",
            expectedFindings = 0,
            source = "fun key(namespace: String, value: String): String = namespace + \":\" + value",
        ),
        RuleCase(
            name = "flags concatenation passed directly to a user facing API",
            expectedFindings = 1,
            source = "fun run(name: String) { println(\"Hello, \" + name) }",
        ),
        RuleCase(
            name = "flags plain text passed directly to a user facing API",
            expectedFindings = 1,
            source = "fun run() { logger.info(\"MineKot started\") }",
        ),
        RuleCase(
            name = "accepts text routed through MiniMessage",
            expectedFindings = 0,
            source = "fun run() { logger.info(miniMessage.deserialize(\"<green>Started</green>\")) }",
        ),
        RuleCase(
            name = "flags raw MiniMessage text passed directly to a logger",
            expectedFindings = 1,
            source = "fun run() { logger.info(\"<green>MineKot started</green>\") }",
        ),
        RuleCase(
            name = "accepts developer exception messages",
            expectedFindings = 0,
            source = "fun requireValue(): Nothing = error(\"Missing generated entity value\")",
        ),
        RuleCase(
            name = "accepts KSP diagnostic logger output",
            expectedFindings = 0,
            source =
                """
                import com.google.devtools.ksp.processing.KSPLogger

                fun report(logger: KSPLogger) { logger.info("Generated event bridge") }
                """,
        ),
        RuleCase(
            name = "accepts KSP diagnostic logger output with wildcard imports",
            expectedFindings = 0,
            source =
                """
                import com.google.devtools.ksp.processing.*

                fun report(logger: KSPLogger, count: Int) {
                    val logMessage = "Generated " + count + " event bridges"
                    logger.info(logMessage)
                }
                """,
        ),
        RuleCase(
            name = "flags Gradle user-facing output not routed through MiniMessage",
            expectedFindings = 3,
            filename = "build.gradle.kts",
            source =
                """
                description = "MineKot build"
                val taskLabel = "publish " + project.name
                println("Configuring ${'$'}{project.name}")
                logger.info("Task ${'$'}{taskLabel}")
                """,
        ),
    )

    @TestFactory
    fun `missing kdoc matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { MissingKDocRule(Config.empty) },
        RuleCase(
            name = "flags undocumented public declarations",
            expectedFindings = 2,
            source =
                """
                class Service {
                    fun run(): String = "MineKot"
                    val name: String = "MineKot"
                }
                """,
        ),
        RuleCase(
            name = "flags an undocumented constructor property",
            expectedFindings = 0,
            source =
                """
                /** Service. */
                class Service(val name: String)
                """,
        ),
        RuleCase(
            name = "accepts a constructor property documented on its class",
            expectedFindings = 0,
            source =
                """
                /**
                 * Service.
                 *
                 * @property name Service name.
                 */
                class Service(val name: String)
                """,
        ),
        RuleCase(
            name = "accepts documented public declarations",
            expectedFindings = 0,
            source =
                """
                /** Service. */
                class Service {
                    /**
                     * Runs the service.
                     *
                     * @return Service result.
                     */
                    fun run(): String = "MineKot"

                    /** Service name. */
                    val name: String = "MineKot"
                }
                """,
        ),
        RuleCase(
            name = "flags incomplete function documentation",
            expectedFindings = 1,
            source =
                """
                /** Service. */
                class Service {
                    /** Runs the service. */
                    fun run(name: String): String = name
                }
                """,
        ),
        RuleCase(
            name = "accepts complete function documentation",
            expectedFindings = 0,
            source =
                """
                /** Service. */
                class Service {
                    /**
                     * Runs the service.
                     *
                     * @param name Service name.
                     * @return Service result.
                     */
                    fun run(name: String): String = name
                }
                """,
        ),
        RuleCase(
            name = "requires private internal and override method docs but ignores locals",
            expectedFindings = 4,
            source =
                """
                private class PrivateService {
                    fun run(): String = "MineKot"
                }

                internal class InternalService

                /** Public service. */
                class PublicService {
                    private fun privateRun(): String = "MineKot"
                    internal fun internalRun(): String = "MineKot"

                    override fun toString(): String {
                        fun localRun(): String = "MineKot"
                        return localRun()
                    }
                }
                """,
        ),
        RuleCase(
            name = "requires docs for complicated non public variables",
            expectedFindings = 2,
            source =
                """
                private val names: List<String> = emptyList()
                internal val lookup: Map<String, Int> = emptyMap()
                private val count: Int = 0
                """,
        ),
        RuleCase(
            name = "requires docs for inherited complicated properties",
            expectedFindings = 1,
            source =
                """
                /** Parent contract. */
                interface Parent { /** Names. */ val names: List<String> }
                /** Child implementation. */
                class Child : Parent { override val names: List<String> = emptyList() }
                """,
        ),
        RuleCase(
            name = "requires docs for inferred complicated properties",
            expectedFindings = 2,
            source =
                """
                private val names = emptyList<String>()
                internal val lookup = mutableMapOf<String, Int>()
                private val count = 0
                """,
        ),
    )

    @Test
    fun `missing kdoc is report only`() {
        val source =
            """
            class Service {
                fun run(name: String): String = name
                val values: List<String> = emptyList()
            }
            """.trimIndent()

        val result = MissingKDocRule(mineKotAutoCorrectConfig).lintAndCorrect(source)

        assertEquals(3, result.findings.size)
        assertEquals(source, result.correctedSource)
    }

    @TestFactory
    fun `coroutine preference matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { CoroutinePreferenceRule(Config.empty) },
        RuleCase(
            name = "flags unqualified JDK thread and timer APIs",
            expectedFindings = 3,
            source =
                """
                fun run() {
                    Thread.sleep(250)
                    Thread { println("MineKot") }
                    Timer()
                }
                """,
        ),
        RuleCase(
            name = "flags fully qualified JDK thread and timer APIs",
            expectedFindings = 3,
            source =
                """
                fun run() {
                    java.lang.Thread.sleep(250)
                    java.lang.Thread { println("MineKot") }
                    java.util.Timer()
                }
                """,
        ),
        RuleCase(
            name = "accepts coroutine delay",
            expectedFindings = 0,
            source = "suspend fun run() { kotlinx.coroutines.delay(250) }",
        ),
        RuleCase(
            name = "flags executors futures and server scheduler calls",
            expectedFindings = 4,
            source =
                """
                fun run() {
                    Executors.newFixedThreadPool(4)
                    CompletableFuture.supplyAsync { "MineKot" }
                    scheduler.runTaskLater(plugin, action, 20)
                    scheduler.scheduleSyncDelayedTask(plugin, action)
                }
                """,
        ),
        RuleCase(
            name = "accepts a platform scheduler bridge into a coroutine scope",
            expectedFindings = 0,
            source =
                """
                fun start() {
                    server.scheduler.runTask(plugin, Runnable { scope.launch { engine.start() } })
                }
                """,
        ),
        RuleCase(
            name = "accepts native scheduler implementation behind platform contract",
            expectedFindings = 0,
            source =
                """
                interface Platform { fun scheduleDelayedTask() }
                class PaperPlatform : Platform {
                    override fun scheduleDelayedTask() {
                        scheduler.runTaskLater(plugin, action, 20)
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts similarly named custom qualified APIs",
            expectedFindings = 0,
            source =
                """
                fun run() {
                    worker.Thread.sleep(250)
                    scheduler.Timer()
                }
                """,
        ),
    )

    @TestFactory
    fun `magic number matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { MagicNumberRule(Config.empty) },
        RuleCase(
            name = "flags nontrivial signed and unsigned numeric forms",
            expectedFindings = 5,
            source = "fun values(): List<Number> = listOf(-7, 100L, 1_000u, 0xff, 10.0f)",
        ),
        RuleCase(
            name = "accepts common numbers and named constants",
            expectedFindings = 0,
            source =
                """
                private const val RETRY_LIMIT = 7
                private val BUFFER_SIZE = 64
                private val retryDelay = 250

                fun values(): List<Int> = listOf(0, 1, 2, RETRY_LIMIT, BUFFER_SIZE, retryDelay)
                """,
        ),
        RuleCase(
            name = "accepts named Java version in Gradle Kotlin DSL",
            expectedFindings = 0,
            filename = "build.gradle.kts",
            source = "val projectJavaVersion = 21",
        ),
        RuleCase(
            name = "accepts numbers in annotations",
            expectedFindings = 0,
            source =
                """
                @Target(AnnotationTarget.FUNCTION)
                annotation class Retry(val count: Int)

                @Retry(7)
                fun run() = Unit
                """,
        ),
    )

    @TestFactory
    fun `result handling matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { ResultHandlingRule(Config.empty) },
        RuleCase(
            name = "flags unsafe access on known result producers",
            expectedFindings = 2,
            source =
                """
                fun first(): String = runCatching { "MineKot" }.getOrThrow()
                fun second(): String = Result.success("MineKot").getOrThrow()
                """,
        ),
        RuleCase(
            name = "flags an ignored run catching statement",
            expectedFindings = 1,
            source =
                """
                fun run() {
                    runCatching { println("MineKot") }
                    println("done")
                }
                """,
        ),
        RuleCase(
            name = "flags an ignored result in a statement branch",
            expectedFindings = 1,
            source =
                """
                fun run(enabled: Boolean) {
                    if (enabled) runCatching { println("MineKot") }
                }
                """,
        ),
        RuleCase(
            name = "accepts explicit result handling",
            expectedFindings = 0,
            source = "fun value(): String = runCatching { \"MineKot\" }.getOrElse { \"fallback\" }",
        ),
        RuleCase(
            name = "accepts a result stored or passed as an argument",
            expectedFindings = 0,
            source =
                """
                fun consume(result: Result<String>) = Unit

                fun run() {
                    val result = runCatching { "MineKot" }
                    consume(runCatching { result.getOrNull().orEmpty() })
                }
                """,
        ),
        RuleCase(
            name = "accepts a result returned through an expression branch",
            expectedFindings = 0,
            source =
                """
                fun value(enabled: Boolean): Result<String> =
                    if (enabled) runCatching { "MineKot" } else Result.failure(IllegalStateException())
                """,
        ),
        RuleCase(
            name = "accepts a result returned from a lambda",
            expectedFindings = 0,
            source =
                """
                fun values(): List<Result<String>> = listOf("MineKot").map { value ->
                    runCatching { value }
                }
                """,
        ),
        RuleCase(
            name = "ignores unrelated get or throw methods",
            expectedFindings = 0,
            source =
                """
                class Registry {
                    fun getOrThrow(): String = "MineKot"
                }

                fun value(registry: Registry): String = registry.getOrThrow()
                """,
        ),
    )

    @TestFactory
    fun `kotlinx preference matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { KotlinxPreferenceRule(Config.empty) },
        RuleCase(
            name = "flags every explicitly blocked Java import",
            expectedFindings = 7,
            source =
                """
                import java.io.File
                import java.lang.Thread
                import java.nio.file.Files
                import java.util.ArrayList
                import java.util.HashMap
                import java.util.HashSet
                import java.util.Timer

                fun run() = Unit
                """,
        ),
        RuleCase(
            name = "flags aliased blocked imports",
            expectedFindings = 1,
            source =
                """
                import java.io.File as JavaFile

                fun path(file: JavaFile): String = file.path
                """,
        ),
        RuleCase(
            name = "flags Java streams futures and JSON imports",
            expectedFindings = 5,
            source =
                """
                import com.google.gson.Gson
                import java.io.InputStream
                import java.io.OutputStream
                import java.util.concurrent.CompletableFuture
                import java.util.concurrent.Executors

                fun run() = Unit
                """,
        ),
        RuleCase(
            name = "accepts Kotlin and unrelated Java APIs",
            expectedFindings = 0,
            source =
                """
                import java.nio.file.Path
                import java.util.UUID
                import kotlin.io.path.name

                fun path(path: Path, id: UUID): String = "${'$'}{path.name}:${'$'}{id}"
                """,
        ),
    )

    @TestFactory
    fun `trailing comma matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { TrailingCommaRule(Config.empty) },
        RuleCase(
            name = "accepts same-line index after multiline receiver",
            expectedFindings = 0,
            source =
                """
                fun value(root: Map<String, String>): String? =
                    root
                        ["version"]
                """,
        ),
        RuleCase(
            name = "flags missing commas in multiline declarations calls and types",
            expectedFindings = 3,
            source =
                """
                fun <T> collect(
                    first: T,
                    second: T
                ): Map<
                    String,
                    T
                > = mapOf(
                    "first" to first,
                    "second" to second
                )
                """,
        ),
        RuleCase(
            name = "accepts trailing commas in multiline lists",
            expectedFindings = 0,
            source =
                """
                fun <T> collect(
                    first: T,
                    second: T,
                ): Map<
                    String,
                    T,
                > = mapOf(
                    "first" to first,
                    "second" to second,
                )
                """,
        ),
        RuleCase(
            name = "accepts single line lists without trailing commas",
            expectedFindings = 0,
            source = "fun sum(first: Int, second: Int): Int = listOf(first, second).sum()",
        ),
        RuleCase(
            name = "accepts single line destructuring with a wrapped initializer",
            expectedFindings = 0,
            source =
                """
                val (first, second) = values
                    ?: error("Missing values")
                """,
        ),
        RuleCase(
            name = "flags multiline type parameters destructuring and indices",
            expectedFindings = 3,
            source =
                """
                fun <
                    First,
                    Second
                > select(values: List<Pair<First, Second>>) {
                    val (
                        first,
                        second
                    ) = values[
                        0,
                        1
                    ]
                }
                """,
        ),
        RuleCase(
            name = "accepts multiline type parameters destructuring and indices with commas",
            expectedFindings = 0,
            source =
                """
                fun <
                    First,
                    Second,
                > select(values: List<Pair<First, Second>>) {
                    val (
                        first,
                        second,
                    ) = values[
                        0,
                        1,
                    ]
                }
                """,
        ),
        RuleCase(
            name = "flags multiline collection literals and context receivers",
            expectedFindings = 2,
            source =
                """
                annotation class Names(val values: Array<String>)

                @Names(
                    [
                        "first",
                        "second"
                    ],
                )
                context(
                    String,
                    Int
                )
                class Named
                """,
        ),
        RuleCase(
            name = "accepts multiline collection literals and context receivers with commas",
            expectedFindings = 0,
            source =
                """
                annotation class Names(val values: Array<String>)

                @Names(
                    [
                        "first",
                        "second",
                    ],
                )
                context(
                    String,
                    Int,
                )
                class Named
                """,
        ),
        RuleCase(
            name = "flags multiline lambda parameters and when conditions",
            expectedFindings = 2,
            source =
                """
                fun select(value: Int, values: List<Int>) {
                    values.fold(0) {
                        accumulator,
                        item
                        -> accumulator + item
                    }
                    when (value) {
                        1,
                        2
                        -> Unit
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts multiline lambda parameters and when conditions with commas",
            expectedFindings = 0,
            source =
                """
                fun select(value: Int, values: List<Int>) {
                    values.fold(0) {
                        accumulator,
                        item,
                        -> accumulator + item
                    }
                    when (value) {
                        1,
                        2,
                        -> Unit
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts single line lambda and when headers with multiline bodies",
            expectedFindings = 0,
            filename = "build.gradle.kts",
            source =
                """
                dependencies
                    .forEach { dependency ->
                        println(dependency)
                    }
                when (dependencyCount) {
                    1 ->
                        println("one")
                }
                """,
        ),
    )

    @TestFactory
    fun `for each preference matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { ForEachPreferenceRule(Config.empty) },
        RuleCase(
            name = "flags straightforward collection iteration",
            expectedFindings = 1,
            source =
                """
                fun send(players: List<String>) {
                    for (player in players) {
                        println(player)
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts collection for each",
            expectedFindings = 0,
            source = "fun send(players: List<String>) = players.forEach { player -> println(player) }",
        ),
        RuleCase(
            name = "accepts loops that use break continue or indices",
            expectedFindings = 0,
            source =
                """
                fun find(players: List<String>) {
                    for (player in players) {
                        if (player == "MineKot") break
                    }
                    for (index in players.indices) {
                        println(index)
                    }
                }
                """,
        ),
        RuleCase(
            name = "does not treat nested loop control as outer loop control",
            expectedFindings = 1,
            source =
                """
                fun send(groups: List<List<String>>) {
                    for (group in groups) {
                        for (player in group) {
                            if (player == "MineKot") break
                        }
                    }
                }
                """,
        ),
    )

    @TestFactory
    fun `gradle dsl conventions matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { GradleDslConventionsRule(Config.empty) },
        RuleCase(
            name = "flags imperative plugins dynamic versions invalid task names and verbose repositories",
            expectedFindings = 4,
            filename = "build.gradle.kts",
            source =
                """
                apply(plugin = "org.jetbrains.kotlin.jvm")
                implementation("com.example:library:1.+")
                tasks.register("generate_docs")
                maven { url = uri("https://repo.example.com") }
                """,
        ),
        RuleCase(
            name = "accepts declarative reproducible Gradle calls",
            expectedFindings = 0,
            filename = "build.gradle.kts",
            source =
                """
                implementation("com.example:library:1.0.0")
                tasks.register("generateDocumentation")
                tasks.register("prepareServerRun")
                tasks.withType<Test>().configureEach { useJUnitPlatform() }
                maven("https://repo.example.com")
                """,
        ),
        RuleCase(
            name = "accepts plugin application inside shared project configuration",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "allprojects { apply(plugin = \"org.minekot.toolchain\") }",
        ),
        RuleCase(
            name = "does not apply Gradle conventions to Kotlin source",
            expectedFindings = 0,
            source =
                """
                fun run() {
                    apply(plugin = "example")
                    implementation("com.example:library:+")
                }
                """,
        ),
        RuleCase(
            name = "flags single line Gradle blocks",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "dependencies { implementation(libs.kotlin.stdlib) }",
        ),
        RuleCase(
            name = "flags short Kotlin dependency notation",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "implementation(kotlin(\"stdlib\"))",
        ),
        RuleCase(
            name = "flags file based project dependencies",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "implementation(files(\"../core/build/libs/core.jar\"))",
        ),
        RuleCase(
            name = "flags eager task configuration",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "tasks.getByName(\"test\")",
        ),
        RuleCase(
            name = "flags camel case task names without an action verb",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "tasks.register(\"documentationGenerator\")",
        ),
        RuleCase(
            name = "flags untyped extension access",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "the<JavaPluginExtension>()",
        ),
        RuleCase(
            name = "flags Java class based extension access",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "extensions.getByType(JavaPluginExtension::class.java)",
        ),
        RuleCase(
            name = "flags invalid extension names",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "extensions.create(\"MineKot\", MineKotExtension::class)",
        ),
        RuleCase(
            name = "flags broad subproject configuration",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source = "subprojects { group = \"org.minekot\" }",
        ),
        RuleCase(
            name = "flags inline task actions",
            expectedFindings = 1,
            filename = "build.gradle.kts",
            source =
                """
                tasks.register("generateDocumentation") {
                    doLast {
                        mkdir("docs")
                        println("done")
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts a trivial inline task action",
            expectedFindings = 0,
            filename = "build.gradle.kts",
            source = "tasks.register(\"generateDocumentation\") { doLast { println(\"done\") } }",
        ),
        RuleCase(
            name = "flags plugin and repository ordering",
            expectedFindings = 2,
            filename = "build.gradle.kts",
            source =
                """
                repositories {
                    mavenCentral()
                    mavenLocal()
                }
                plugins {
                    id("minekot.module")
                }
                """,
        ),
    )

    @TestFactory
    fun `import policy matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { ImportPolicyRule(Config.empty) },
        RuleCase(
            name = "accepts imports in any order",
            expectedFindings = 0,
            source =
                """
                import kotlin.io.path.name
                import java.io.File

                fun run() = Unit
                """,
        ),
        RuleCase(
            name = "flags five imports from one package",
            expectedFindings = 1,
            source =
                """
                import org.example.Alpha
                import org.example.Bravo
                import org.example.Charlie
                import org.example.Delta
                import org.example.Echo

                fun run() = Unit
                """,
        ),
        RuleCase(
            name = "accepts imports from an unresolved object or member scope",
            expectedFindings = 0,
            source =
                """
                import org.example.Service.first
                import org.example.Service.second
                import org.example.Service.third

                fun run() = Unit
                """,
        ),
        RuleCase(
            name = "flags direct nested class imports",
            expectedFindings = 1,
            source =
                """
                import org.example.Outer.Nested

                fun run() = Unit
                """,
        ),
        RuleCase(
            name = "requires on demand packages to use wildcards",
            expectedFindings = 1,
            source =
                """
                import java.util.UUID

                fun run() = Unit
                """,
        ),
        RuleCase(
            name = "does not treat java util subpackages as on demand",
            expectedFindings = 0,
            source =
                """
                import java.util.concurrent.CompletableFuture

                fun run(): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)
                """,
        ),
        RuleCase(
            name = "accepts ordered threshold wildcard imports",
            expectedFindings = 0,
            source =
                """
                import java.util.*
                import java.io.File
                import javax.inject.Inject
                import kotlin.io.path.name
                import org.example.Service

                fun run() = Unit
                """,
        ),
    )

    @TestFactory
    fun `source file policy matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { SourceFilePolicyRule(Config.empty) },
        RuleCase(
            name = "flags an unclosed formatter region",
            expectedFindings = 1,
            source = "// @formatter:off\r\nfun run() = Unit\r\n\r\n",
        ),
        RuleCase(
            name = "flags unmatched formatter on",
            expectedFindings = 1,
            source = "// @formatter:on\nfun run() = Unit\n",
        ),
        RuleCase(
            name = "accepts LF and balanced formatter regions",
            expectedFindings = 0,
            source = "// @formatter:off\nfun run()=Unit\n// @formatter:on\n",
        ),
    )

    @TestFactory
    fun `comment formatting matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { CommentFormattingRule(Config.empty) },
        RuleCase(
            name = "flags indented and padded line comments",
            expectedFindings = 2,
            source = "fun run() {\n    // explanation\n}\n",
        ),
        RuleCase(
            name = "accepts compact first-column and formatter comments",
            expectedFindings = 0,
            source = "//explanation\n// @formatter:off\nfun run()=Unit\n// @formatter:on\n",
        ),
        RuleCase(
            name = "does not apply line comment formatting to KDoc",
            expectedFindings = 0,
            source = "/** Documentation. */\nfun run() = Unit\n",
        ),
    )

    @TestFactory
    fun `explicit scope matrix`(): List<DynamicTest> = ruleCases(
        ruleFactory = { ExplicitScopeInNestedScopeRule(Config.empty) },
        RuleCase(
            name = "flags unqualified outer property and method refs",
            expectedFindings = 2,
            source =
                """
                class Engine {
                    val name: String = "MineKot"
                    fun start() = Unit
                    fun run() {
                        runCatching {
                            println(name)
                            start()
                        }
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts labeled outer refs",
            expectedFindings = 0,
            source =
                """
                class Engine {
                    val name: String = "MineKot"
                    fun start() = Unit
                    fun run() {
                        runCatching {
                            println(this@Engine.name)
                            this@Engine.start()
                        }
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts lambda params locals and other receivers",
            expectedFindings = 0,
            source =
                """
                class Engine {
                    val name: String = "MineKot"
                    fun run(values: List<String>) {
                        values.forEach { name ->
                            val run = name.length
                            println(name)
                            other.name
                        }
                    }
                }
                """,
        ),
        RuleCase(
            name = "accepts nested class and enum references",
            expectedFindings = 0,
            source =
                """
                class Engine {
                    enum class State { STARTED }
                    fun run() = runCatching { println(State.STARTED) }
                }
                """,
        ),
    )

    @Test
    fun `string template braces auto correction is complete and idempotent`() {
        val source = "fun greeting(first: String, last: String): String = \"Hello ${'$'}first ${'$'}last\""
        val firstPass = StringTemplateBracesRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = StringTemplateBracesRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(2, firstPass.findings.size)
        assertEquals(
            "fun greeting(first: String, last: String): String = \"Hello ${'$'}{first} ${'$'}{last}\"",
            firstPass.correctedSource,
        )
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `trailing comma auto correction is complete and idempotent`() {
        val source =
            """
            fun sum(
                first: Int,
                second: Int // retained comment
            ): Int {
                val total = listOf(
                    first,
                    second
                ).sum()
                val folded = listOf(1, 2).fold(0) {
                    accumulator,
                    item
                    -> accumulator + item
                }
                when (total) {
                    1,
                    2
                    -> Unit
                }
                return folded
            }
            """.trimIndent()
        val firstPass = TrailingCommaRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = TrailingCommaRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(4, firstPass.findings.size)
        assertTrue(
            firstPass.correctedSource.contains("second: Int, // retained comment"),
            firstPass.correctedSource,
        )
        assertTrue(firstPass.correctedSource.contains("second,"))
        assertTrue(firstPass.correctedSource.contains("item,"))
        assertTrue(firstPass.correctedSource.contains("2,"))
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `import ordering is not enforced`() {
        val source =
            """
            import com.google.devtools.ksp.processing.*
            import com.google.devtools.ksp.symbol.*
            import com.squareup.kotlinpoet.*
            import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
            import com.squareup.kotlinpoet.ksp.toClassName
            import com.squareup.kotlinpoet.ksp.toTypeName
            import org.minekot.codegen.ksp.*

            fun run(): Unit = Unit
            """.trimIndent()
        val firstPass = ImportPolicyRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = ImportPolicyRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(0, firstPass.findings.size)
        assertEquals(source, firstPass.correctedSource)
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `wildcard import auto correction is complete and idempotent`() {
        val source =
            """
            import java.util.UUID

            fun id(): UUID = UUID.randomUUID()
            """.trimIndent()
        val firstPass = ImportPolicyRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = ImportPolicyRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(1, firstPass.findings.size)
        assertTrue(firstPass.correctedSource.contains("import java.util.*"))
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `wildcard import auto correction never creates an object star import`() {
        val source =
            """
            import org.example.Service.first
            import org.example.Service.second
            import org.example.Service.third

            fun run(): Unit = Unit
            """.trimIndent()
        val result = ImportPolicyRule(mineKotAutoCorrectConfig).lintAndCorrect(source)

        assertEquals(0, result.findings.size)
        assertEquals(source, result.correctedSource)
    }

    @Test
    fun `comment formatting auto correction is complete and idempotent`() {
        val source = "    // explanation\nfun run(): Unit = Unit"
        val firstPass = CommentFormattingRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = CommentFormattingRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(2, firstPass.findings.size)
        assertTrue(firstPass.correctedSource.startsWith("//explanation\n"))
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `source BOM auto correction is complete and idempotent`() {
        val source = "\uFEFFfun run(): Unit = Unit"
        val firstPass = SourceFilePolicyRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = SourceFilePolicyRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(1, firstPass.findings.size)
        assertEquals("fun run(): Unit = Unit", firstPass.correctedSource)
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `Gradle repository auto correction is complete and idempotent`() {
        val source =
            """
            repositories {
                maven { url = uri("https://repo.example.com") }
            }
            """.trimIndent()
        val firstPass = GradleDslConventionsRule(mineKotAutoCorrectConfig)
            .lintAndCorrect(source, "build.gradle.kts")
        val secondPass = GradleDslConventionsRule(mineKotAutoCorrectConfig)
            .lintAndCorrect(firstPass.correctedSource, "build.gradle.kts")

        assertEquals(1, firstPass.findings.size)
        assertTrue(firstPass.correctedSource.contains("maven(\"https://repo.example.com\")"))
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `Kotlinx path auto correction updates calls and imports idempotently`() {
        val source =
            """
            import java.nio.file.Files
            import java.nio.file.Path

            fun inspect(path: Path): Boolean {
                Files.newInputStream(path)
                Files.newOutputStream(path)
                Files.deleteIfExists(path)
                return Files.exists(path)
            }
            """.trimIndent()
        val firstPass = KotlinxPreferenceRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = KotlinxPreferenceRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(1, firstPass.findings.size)
        assertTrue(!firstPass.correctedSource.contains("java.nio.file.Files"))
        assertTrue(firstPass.correctedSource.contains("path.inputStream()"))
        assertTrue(firstPass.correctedSource.contains("path.outputStream()"))
        assertTrue(firstPass.correctedSource.contains("path.deleteIfExists()"))
        assertTrue(firstPass.correctedSource.contains("path.exists()"))
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `Kotlinx path auto correction preserves unsupported overload imports`() {
        val source =
            """
            import java.nio.file.Files
            import java.nio.file.Path

            fun inspect(path: Path): Unit {
                Files.copy(path, path)
                Files.exists(path)
            }
            """.trimIndent()
        val result = KotlinxPreferenceRule(mineKotAutoCorrectConfig).lintAndCorrect(source)

        assertTrue(result.correctedSource.contains("import java.nio.file.Files"))
        assertTrue(result.correctedSource.contains("Files.copy(path, path)"))
        assertTrue(result.correctedSource.contains("path.exists()"))
    }

    @Test
    fun `ignored Result auto correction is complete and idempotent`() {
        val source = "fun run(): Unit { runCatching { println(\"work\") } }"
        val firstPass = ResultHandlingRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = ResultHandlingRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(1, firstPass.findings.size)
        assertEquals("fun run(): Unit { runCatching { println(\"work\") }.getOrNull() }", firstPass.correctedSource)
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `Result handling recognizes discarded Unit lambda results`() {
        val source = "fun run(values: List<Int>): Unit { values.forEach { runCatching { println(it) } } }"
        val result = ResultHandlingRule(mineKotAutoCorrectConfig).lintAndCorrect(source)

        assertEquals(1, result.findings.size)
        assertTrue(result.correctedSource.contains("runCatching { println(it) }.getOrNull()"))
    }

    @Test
    fun `forEach auto correction preserves body and rejects mutation`() {
        val source =
            """
            fun send(players: List<String>): Unit {
                for (player in players) {
                    println(player)
                }
            }
            """.trimIndent()
        val firstPass = ForEachPreferenceRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = ForEachPreferenceRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(1, firstPass.findings.size)
        assertTrue(firstPass.correctedSource.contains("players.forEach { player ->"))
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
        val mutating = "fun trim(values: MutableList<Int>) { for (value in values) { values.remove(value) } }"
        assertEquals(0, ForEachPreferenceRule(mineKotAutoCorrectConfig).lintAndCorrect(mutating).findings.size)
        val indexedMutation = "fun trim(values: MutableList<Int>) { for (value in values) { values[0] = value } }"
        assertEquals(0, ForEachPreferenceRule(mineKotAutoCorrectConfig).lintAndCorrect(indexedMutation).findings.size)
    }

    @Test
    fun `explicit receiver auto correction requires one valid owner`() {
        val source =
            """
            class Engine {
                val name: String = "MineKot"

                fun run(): Unit {
                    listOf(1).forEach { println(name) }
                }
            }
            """.trimIndent()
        val firstPass = ExplicitScopeInNestedScopeRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = ExplicitScopeInNestedScopeRule(mineKotAutoCorrectConfig)
            .lintAndCorrect(firstPass.correctedSource)

        assertEquals(1, firstPass.findings.size)
        assertTrue(firstPass.correctedSource.contains("println(this@Engine.name)"))
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
    }

    @Test
    fun `line wrapping corrects long calls and preserves string literals`() {
        val source =
            "fun run(): Unit = consume(firstArgument, secondArgument, thirdArgument, fourthArgument, fifthArgument, sixthArgument, seventhArgument)"
        val firstPass = LineWrappingRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
        val secondPass = LineWrappingRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

        assertEquals(1, firstPass.findings.size)
        assertTrue(firstPass.correctedSource.contains("consume(\n"))
        assertTrue(firstPass.correctedSource.contains("        firstArgument,"))
        assertEquals(0, secondPass.findings.size)
        assertEquals(firstPass.correctedSource, secondPass.correctedSource)
        val longLiteral = "fun text(): String = \"${"x".repeat(130)}\""
        assertEquals(0, LineWrappingRule(mineKotAutoCorrectConfig).lintAndCorrect(longLiteral).findings.size)
    }

    @Test
    fun `line wrapping corrects getters casts and when entries idempotently`() {
        val sources = listOf(
            "val exceptionallyLongDescriptivePropertyNameForWrapping: String get() = firstComponent + secondComponent + thirdComponent + fourthComponent",
            "fun cast(value: Any): ExtremelyLongDescriptiveDomainType = value as ExtremelyLongDescriptiveDomainTypeWithEnoughCharactersToRequireWrapping",
            "fun select(value: Int): String = when (value) { firstExtremelyLongCondition, secondExtremelyLongCondition, thirdExtremelyLongCondition -> \"match\"; else -> \"other\" }",
        )

        sources.forEach { source ->
            val firstPass = LineWrappingRule(mineKotAutoCorrectConfig).lintAndCorrect(source)
            val secondPass = LineWrappingRule(mineKotAutoCorrectConfig).lintAndCorrect(firstPass.correctedSource)

            assertEquals(1, firstPass.findings.size, source)
            assertTrue(firstPass.correctedSource.contains('\n'), firstPass.correctedSource)
            assertEquals(0, secondPass.findings.size, secondPass.correctedSource)
            assertEquals(firstPass.correctedSource, secondPass.correctedSource)
        }
    }

    @Test
    fun `new automatic corrections preserve formatter regions comments aliases and partial syntax`() {
        val formatterControlled =
            "// @formatter:off\nfun run(): Unit { runCatching { println(\"work\") } }\n// @formatter:on"
        val commentedCall =
            "import java.nio.file.Files\nimport java.nio.file.Path\nfun inspect(path: Path) = Files.exists(/* preserve */ path)"
        val aliasedExtension =
            "import java.nio.file.Files\nimport java.nio.file.Path\nimport kotlin.io.path.exists as pathExists\nfun inspect(path: Path) = Files.exists(path)"
        val partial = "fun run(): Unit { runCatching { println(\"work\") }"

        assertEquals(
            formatterControlled,
            ResultHandlingRule(mineKotAutoCorrectConfig).lintAndCorrect(formatterControlled).correctedSource,
        )
        assertEquals(
            commentedCall,
            KotlinxPreferenceRule(mineKotAutoCorrectConfig).lintAndCorrect(commentedCall).correctedSource,
        )
        val correctedAlias = KotlinxPreferenceRule(mineKotAutoCorrectConfig).lintAndCorrect(aliasedExtension).correctedSource
        assertTrue(correctedAlias.contains("import kotlin.io.path.exists\n"), correctedAlias)
        assertTrue(correctedAlias.contains("import kotlin.io.path.exists as pathExists"), correctedAlias)
        assertEquals(partial, ResultHandlingRule(mineKotAutoCorrectConfig).lintAndCorrect(partial).correctedSource)
    }

    private fun ruleCases(
        ruleFactory: () -> Rule,
        vararg cases: RuleCase,
    ): List<DynamicTest> = cases.map { ruleCase ->
        DynamicTest.dynamicTest(ruleCase.name) {
            val findings = ruleFactory().lint(ruleCase.source.trimIndent(), ruleCase.filename)

            assertEquals(
                ruleCase.expectedFindings,
                findings.size,
                "${ruleCase.name}: ${findings.joinToString { finding -> finding.message }}",
            )
        }
    }

    private data class RuleCase(
        val name: String,
        val expectedFindings: Int,
        val source: String,
        val filename: String = "Test.kt",
    )

    private data class ProviderRuleCase(
        val violatingSource: String,
        val cleanSource: String,
        val filename: String = "Test.kt",
    )

    private data class KtlintRuleCase(
        val violatingSource: String,
        val cleanSource: String,
    )

    private companion object {
        private val providerRuleCases: Map<String, ProviderRuleCase> = mapOf(
            "ForbiddenTryCatch" to ProviderRuleCase(
                "fun run() { try { Unit } catch (error: Exception) { Unit } }",
                "fun run(): Result<Unit> = runCatching { Unit }",
            ),
            "StringTemplateBraces" to ProviderRuleCase(
                "fun text(name: String): String = \"Hello ${'$'}name\"",
                "fun text(name: String): String = \"Hello ${'$'}{name}\"",
            ),
            "MiniMessageText" to ProviderRuleCase(
                "fun run() { println(\"Hello\") }",
                "fun run() { logger.info(mineKotMiniMessage(\"<green>Hello</green>\")) }",
            ),
            "MissingKDoc" to ProviderRuleCase(
                "fun run(): Unit = Unit",
                "/** Runs. */\nfun run(): Unit = Unit",
            ),
            "CoroutinePreference" to ProviderRuleCase(
                "fun run() { Thread.sleep(10) }",
                "suspend fun run() { kotlinx.coroutines.delay(10) }",
            ),
            "MagicNumber" to ProviderRuleCase(
                "fun value(): Int = 42",
                "val projectJavaVersion = 21",
                filename = "build.gradle.kts",
            ),
            "ResultHandling" to ProviderRuleCase(
                "fun run() { runCatching { Unit } }",
                "fun run(): Result<Unit> = runCatching { Unit }",
            ),
            "KotlinxPreference" to ProviderRuleCase(
                "import java.io.File\nfun run() = Unit",
                "import java.nio.file.Path\nfun run(path: Path) = Unit",
            ),
            "TrailingComma" to ProviderRuleCase(
                "fun run() = listOf(\n    1,\n    2\n)",
                "fun run() = listOf(\n    1,\n    2,\n)",
            ),
            "LineWrapping" to ProviderRuleCase(
                "fun run(): Unit = consume(firstArgument, secondArgument, thirdArgument, fourthArgument, fifthArgument, sixthArgument, seventhArgument)",
                "fun run(): Unit = consume(firstArgument, secondArgument)",
            ),
            "ForEachPreference" to ProviderRuleCase(
                "fun run(values: List<Int>) { for (value in values) { println(value) } }",
                "fun run(values: List<Int>) { values.forEach { value -> println(value) } }",
            ),
            "GradleDslConventions" to ProviderRuleCase(
                "apply(plugin = \"kotlin\")",
                "implementation(\"org.example:library:1.0.0\")",
                "build.gradle.kts",
            ),
            "ImportPolicy" to ProviderRuleCase(
                "import org.example.Outer.Nested\nfun run() = Unit",
                "import java.util.concurrent.CompletableFuture\nfun run() = Unit",
            ),
            "SourceFilePolicy" to ProviderRuleCase(
                "// @formatter:off\nfun run() = Unit",
                "// @formatter:off\nfun run() = Unit\n// @formatter:on",
            ),
            "CommentFormatting" to ProviderRuleCase(
                "// padded\nfun run() = Unit",
                "//compact\nfun run() = Unit",
            ),
            "ExplicitScopeInNestedScope" to ProviderRuleCase(
                "class Engine { val name = \"MineKot\"; fun run() { runCatching { println(name) } } }",
                "class Engine { val name = \"MineKot\"; fun run() { runCatching { println(this@Engine.name) } } }",
            ),
        )

        private val ktlintRuleCases: Map<String, KtlintRuleCase> = mapOf(
            "Indentation" to KtlintRuleCase(
                "fun run() {\n  Unit\n}",
                "fun run() {\n    Unit\n}",
            ),
            "NoBlankLineBeforeRbrace" to KtlintRuleCase(
                "fun run() {\n    Unit\n\n}",
                "fun run() {\n    Unit\n}",
            ),
            "NoConsecutiveBlankLines" to KtlintRuleCase(
                "val first = 1\n\n\nval second = 2",
                "val first = 1\n\nval second = 2",
            ),
            "NoEmptyFirstLineInClassBody" to KtlintRuleCase(
                "class Service {\n\n    val name = \"MineKot\"\n}",
                "class Service {\n    val name = \"MineKot\"\n}",
            ),
            "NoEmptyFirstLineInMethodBlock" to KtlintRuleCase(
                "fun run() {\n\n    Unit\n}",
                "fun run() {\n    Unit\n}",
            ),
            "NoMultipleSpaces" to KtlintRuleCase(
                "val  value = 1",
                "val value = 1",
            ),
            "NoTrailingSpaces" to KtlintRuleCase(
                "val value = 1  \n",
                "val value = 1\n",
            ),
            "SpacingAroundColon" to KtlintRuleCase(
                "val value:Int = 1",
                "val value: Int = 1",
            ),
            "SpacingAroundComma" to KtlintRuleCase(
                "fun run() = listOf(1,2)",
                "fun run() = listOf(1, 2)",
            ),
            "SpacingAroundCurly" to KtlintRuleCase(
                "fun run() { if (true){ Unit } }",
                "fun run() { if (true) { Unit } }",
            ),
            "SpacingAroundOperators" to KtlintRuleCase(
                "val value = 1+2",
                "val value = 1 + 2",
            ),
            "SpacingAroundRangeOperator" to KtlintRuleCase(
                "val values = 1 .. 2",
                "val values = 1..2",
            ),
            "SpacingAroundUnaryOperator" to KtlintRuleCase(
                "val value = ! true",
                "val value = !true",
            ),
            "SpacingBetweenDeclarationsWithAnnotations" to KtlintRuleCase(
                "val first = 1\n@Deprecated(\"old\")\nval second = 2",
                "val first = 1\n\n@Deprecated(\"old\")\nval second = 2",
            ),
            "SpacingBetweenDeclarationsWithComments" to KtlintRuleCase(
                "val first = 1\n//second\nval second = 2",
                "val first = 1\n\n//second\nval second = 2",
            ),
        )
    }
}
