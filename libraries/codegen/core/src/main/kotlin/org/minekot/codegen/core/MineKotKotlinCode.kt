package org.minekot.codegen.core

import com.squareup.kotlinpoet.*

/** Name used by a generated Kotlin declaration or expression. */
@JvmInline
value class MineKotKotlinName(val value: String)

/** Typed expression rendered as Kotlin source by [renderMineKotExpression]. */
sealed interface MineKotKotlinExpression

/** Reference to a generated Kotlin name. */
data class MineKotReference(val name: MineKotKotlinName) : MineKotKotlinExpression

/** Reference to an import-aware KotlinPoet member. */
data class MineKotMemberReference(val member: MemberName) : MineKotKotlinExpression

/** Reference to an import-aware Kotlin type. */
data class MineKotTypeReference(val type: TypeName) : MineKotKotlinExpression

/** Kotlin string literal. */
data class MineKotStringLiteral(val value: String) : MineKotKotlinExpression

/** Kotlin Boolean literal. */
data class MineKotBooleanLiteral(val value: Boolean) : MineKotKotlinExpression

/** Kotlin numeric literal with explicit source representation. */
data class MineKotNumericLiteral(
    val value: Number,
    val kind: MineKotNumericKind,
) : MineKotKotlinExpression

/** Supported Kotlin numeric literal representations. */
enum class MineKotNumericKind {
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
}

/** Kotlin null literal. */
data object MineKotNullLiteral : MineKotKotlinExpression

/** Anonymous object implementing [type]. */
data class MineKotObjectExpression(val type: TypeName) : MineKotKotlinExpression

/** Member access on [receiver]. */
data class MineKotMemberAccess(
    val receiver: MineKotKotlinExpression,
    val member: MineKotKotlinName,
) : MineKotKotlinExpression

/** Null-safe member access on [receiver]. */
data class MineKotSafeMemberAccess(
    val receiver: MineKotKotlinExpression,
    val member: MineKotKotlinName,
) : MineKotKotlinExpression

/** Function, constructor, or invoke-operator call. */
data class MineKotCall(
    val target: MineKotKotlinExpression,
    val arguments: List<MineKotKotlinExpression> = emptyList(),
    val namedArguments: Map<MineKotKotlinName, MineKotKotlinExpression> = emptyMap(),
    val typeArguments: List<TypeName> = emptyList(),
    val lambda: MineKotLambda? = null,
) : MineKotKotlinExpression

/** Kotlin lambda expression. */
data class MineKotLambda(
    val parameters: List<MineKotKotlinName>,
    val statements: List<MineKotKotlinStatement>,
) : MineKotKotlinExpression

/** Kotlin `is` type check. */
data class MineKotIsCheck(
    val expression: MineKotKotlinExpression,
    val type: TypeName,
) : MineKotKotlinExpression

/** Kotlin Elvis expression. */
data class MineKotElvis(
    val expression: MineKotKotlinExpression,
    val fallback: MineKotKotlinExpression,
) : MineKotKotlinExpression

/** Kotlin class literal. */
data class MineKotClassLiteral(val type: TypeName) : MineKotKotlinExpression

/** Kotlin labeled return expression. */
data class MineKotLabeledReturn(val label: MineKotKotlinName) : MineKotKotlinExpression

/** Kotlin cast expression. */
data class MineKotCast(
    val expression: MineKotKotlinExpression,
    val type: TypeName,
) : MineKotKotlinExpression

/** Kotlin indexed access expression. */
data class MineKotIndexAccess(
    val receiver: MineKotKotlinExpression,
    val index: MineKotKotlinExpression,
) : MineKotKotlinExpression

/** Kotlin callable reference expression. */
data class MineKotCallableReference(
    val receiver: MineKotKotlinExpression? = null,
    val member: MineKotKotlinName,
) : MineKotKotlinExpression

/** Kotlin `this` expression, optionally qualified by [label]. */
data class MineKotThisReference(val label: MineKotKotlinName? = null) : MineKotKotlinExpression

/** Kotlin binary expression. */
data class MineKotBinary(
    val left: MineKotKotlinExpression,
    val operator: MineKotBinaryOperator,
    val right: MineKotKotlinExpression,
) : MineKotKotlinExpression

/** Supported Kotlin binary operators. */
enum class MineKotBinaryOperator {
    EQUALS,
    NOT_EQUALS,
    LESS_THAN,
    LESS_OR_EQUAL,
    GREATER_THAN,
    GREATER_OR_EQUAL,
    TO,
}

/** Kotlin unary expression. */
data class MineKotUnary(
    val operator: MineKotUnaryOperator,
    val expression: MineKotKotlinExpression,
) : MineKotKotlinExpression

/** Supported Kotlin unary operators. */
enum class MineKotUnaryOperator {
    NEGATE,
    NOT,
}

/** Kotlin throw expression. */
data class MineKotThrow(val expression: MineKotKotlinExpression) : MineKotKotlinExpression

/** Typed statement rendered as Kotlin source by [renderMineKotCode]. */
sealed interface MineKotKotlinStatement

/** Immutable local variable declaration. */
data class MineKotVariable(
    val name: MineKotKotlinName,
    val initializer: MineKotKotlinExpression,
) : MineKotKotlinStatement

/** Expression used as a statement. */
data class MineKotExpressionStatement(val expression: MineKotKotlinExpression) : MineKotKotlinStatement

/** Kotlin `if` statement. */
data class MineKotIf(
    val condition: MineKotKotlinExpression,
    val statements: List<MineKotKotlinStatement>,
) : MineKotKotlinStatement

/** Kotlin return statement. */
data class MineKotReturn(val expression: MineKotKotlinExpression) : MineKotKotlinStatement

/** Kotlin assignment statement. */
data class MineKotAssignment(
    val target: MineKotKotlinExpression,
    val operator: MineKotAssignmentOperator,
    val value: MineKotKotlinExpression,
) : MineKotKotlinStatement

/** Supported Kotlin assignment operators. */
enum class MineKotAssignmentOperator {
    ASSIGN,
    PLUS_ASSIGN,
}

/**
 * Renders typed Kotlin statements for KotlinPoet.
 *
 * @param statements Statements to render.
 * @return KotlinPoet code block.
 */
fun renderMineKotCode(statements: List<MineKotKotlinStatement>): CodeBlock =
    CodeBlock.builder().apply { statements.forEach(::addMineKotStatement) }.build()

/**
 * Applies a typed initializer to a generated property.
 *
 * @param expression Initializer expression.
 * @return This property builder.
 */
fun PropertySpec.Builder.initializer(expression: MineKotKotlinExpression): PropertySpec.Builder =
    initializer(renderMineKotExpression(expression))

/**
 * Applies a typed default value to a generated parameter.
 *
 * @param expression Default expression.
 * @return This parameter builder.
 */
fun ParameterSpec.Builder.defaultValue(expression: MineKotKotlinExpression): ParameterSpec.Builder =
    defaultValue(renderMineKotExpression(expression))

/**
 * Renders one typed Kotlin expression for KotlinPoet.
 *
 * @param expression Expression to render.
 * @return KotlinPoet code block.
 */
fun renderMineKotExpression(expression: MineKotKotlinExpression): CodeBlock =
    CodeBlock.builder().apply { addMineKotExpression(expression) }.build()

private fun CodeBlock.Builder.addMineKotStatement(statement: MineKotKotlinStatement) {
    when (statement) {
        is MineKotVariable -> {
            add("val %N = ", statement.name.value)
            addMineKotExpression(statement.initializer)
            add("\n")
        }

        is MineKotExpressionStatement -> {
            addMineKotExpression(statement.expression)
            add("\n")
        }

        is MineKotIf -> {
            add("if (")
            addMineKotExpression(statement.condition)
            add(") {\n")
            indent()
            statement.statements.forEach(::addMineKotStatement)
            unindent()
            add("}\n")
        }

        is MineKotReturn -> {
            add("return ")
            addMineKotExpression(statement.expression)
            add("\n")
        }

        is MineKotAssignment -> {
            addMineKotExpression(statement.target)
            add(
                when (statement.operator) {
                    MineKotAssignmentOperator.ASSIGN -> " = "
                    MineKotAssignmentOperator.PLUS_ASSIGN -> " += "
                },
            )
            addMineKotExpression(statement.value)
            add("\n")
        }
    }
}

private fun CodeBlock.Builder.addMineKotExpression(expression: MineKotKotlinExpression) {
    when (expression) {
        is MineKotReference -> add("%N", expression.name.value)
        is MineKotMemberReference -> add("%M", expression.member)
        is MineKotTypeReference -> add("%T", expression.type)
        is MineKotStringLiteral -> add("%S", expression.value)
        is MineKotBooleanLiteral -> add("%L", expression.value)
        is MineKotNumericLiteral -> addMineKotNumericLiteral(expression)
        MineKotNullLiteral -> add("null")
        is MineKotObjectExpression -> add("object : %T {}", expression.type)
        is MineKotMemberAccess -> {
            addMineKotExpression(expression.receiver)
            add(".%N", expression.member.value)
        }

        is MineKotSafeMemberAccess -> {
            addMineKotExpression(expression.receiver)
            add("?.%N", expression.member.value)
        }

        is MineKotCall -> addMineKotCall(expression)
        is MineKotLambda -> addMineKotLambda(expression)
        is MineKotIsCheck -> {
            addMineKotExpression(expression.expression)
            add(" is %T", expression.type)
        }

        is MineKotElvis -> {
            addMineKotExpression(expression.expression)
            add(" ?: ")
            addMineKotExpression(expression.fallback)
        }

        is MineKotClassLiteral -> add("%T::class", expression.type)
        is MineKotLabeledReturn -> add("return@%N", expression.label.value)
        is MineKotCast -> {
            addMineKotExpression(expression.expression)
            add(" as %T", expression.type)
        }

        is MineKotIndexAccess -> {
            addMineKotExpression(expression.receiver)
            add("[")
            addMineKotExpression(expression.index)
            add("]")
        }

        is MineKotCallableReference -> {
            expression.receiver?.let { receiver -> addMineKotExpression(receiver) }
            add("::%N", expression.member.value)
        }

        is MineKotThisReference -> {
            add("this")
            expression.label?.let { label -> add("@%N", label.value) }
        }

        is MineKotBinary -> {
            addMineKotExpression(expression.left)
            add(" ${expression.operator.source} ")
            addMineKotExpression(expression.right)
        }

        is MineKotUnary -> {
            add(expression.operator.source)
            addMineKotExpression(expression.expression)
        }

        is MineKotThrow -> {
            add("throw ")
            addMineKotExpression(expression.expression)
        }
    }
}

private val MineKotBinaryOperator.source: String
    get() = when (this) {
        MineKotBinaryOperator.EQUALS -> "=="
        MineKotBinaryOperator.NOT_EQUALS -> "!="
        MineKotBinaryOperator.LESS_THAN -> "<"
        MineKotBinaryOperator.LESS_OR_EQUAL -> "<="
        MineKotBinaryOperator.GREATER_THAN -> ">"
        MineKotBinaryOperator.GREATER_OR_EQUAL -> ">="
        MineKotBinaryOperator.TO -> "to"
    }

private val MineKotUnaryOperator.source: String
    get() = when (this) {
        MineKotUnaryOperator.NEGATE -> "-"
        MineKotUnaryOperator.NOT -> "!"
    }

private fun CodeBlock.Builder.addMineKotNumericLiteral(literal: MineKotNumericLiteral) {
    val source = when (literal.kind) {
        MineKotNumericKind.BYTE,
        MineKotNumericKind.SHORT,
        MineKotNumericKind.INT,
            -> literal.value.toInt().toString()

        MineKotNumericKind.LONG -> "${literal.value.toLong()}L"
        MineKotNumericKind.FLOAT -> "${literal.value.toFloat()}f"
        MineKotNumericKind.DOUBLE -> literal.value.toDouble().toString()
    }
    add("%L", source)
}

private fun CodeBlock.Builder.addMineKotCall(call: MineKotCall) {
    addMineKotExpression(call.target)
    if (call.typeArguments.isNotEmpty()) {
        add("<")
        call.typeArguments.forEachIndexed { index, type ->
            if (index > 0) add(", ")
            add("%T", type)
        }
        add(">")
    }
    if (call.arguments.isNotEmpty() || call.namedArguments.isNotEmpty() || call.lambda == null) {
        add("(")
        var argumentIndex = 0
        call.arguments.forEach { argument ->
            if (argumentIndex++ > 0) add(", ")
            addMineKotExpression(argument)
        }
        call.namedArguments.forEach { (name, argument) ->
            if (argumentIndex++ > 0) add(", ")
            add("%N = ", name.value)
            addMineKotExpression(argument)
        }
        add(")")
    }
    call.lambda?.let { lambda ->
        add(" ")
        addMineKotLambda(lambda)
    }
}

private fun CodeBlock.Builder.addMineKotLambda(lambda: MineKotLambda) {
    add("{")
    if (lambda.parameters.isNotEmpty()) {
        add(" ")
        lambda.parameters.forEachIndexed { index, parameter ->
            if (index > 0) add(", ")
            add("%N", parameter.value)
        }
        add(" ->")
    }
    add("\n")
    indent()
    lambda.statements.forEach(::addMineKotStatement)
    unindent()
    add("}")
}
