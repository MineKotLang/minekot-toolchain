package org.minekot.codegen.core

import com.squareup.kotlinpoet.TypeName

/**
 * Function model rendered by MineKot code generation helpers.
 *
 * @property name Generated function name.
 * @property kdoc Generated function documentation.
 * @property returnType Generated function return type.
 * @property returnValue Literal string returned by the generated function.
 */
data class MineKotGeneratedFunction(
    val name: String,
    val kdoc: String,
    val returnType: TypeName,
    val returnValue: String,
)

/**
 * Property model rendered by MineKot code generation helpers.
 *
 * @property name Generated property name.
 * @property kdoc Generated property documentation.
 * @property type Generated property type.
 */
data class MineKotGeneratedProperty(
    val name: String,
    val kdoc: String,
    val type: TypeName,
)

/**
 * Data class model rendered by MineKot code generation helpers.
 *
 * @property packageName Generated package name.
 * @property name Generated class name.
 * @property kdoc Generated class documentation.
 * @property properties Constructor properties.
 */
data class MineKotGeneratedDataClass(
    val packageName: String,
    val name: String,
    val kdoc: String,
    val properties: List<MineKotGeneratedProperty>,
)

/**
 * Object model rendered by MineKot code generation helpers.
 *
 * @property packageName Generated package name.
 * @property name Generated object name.
 * @property kdoc Generated object documentation.
 * @property functions Generated functions.
 */
data class MineKotGeneratedObject(
    val packageName: String,
    val name: String,
    val kdoc: String,
    val functions: List<MineKotGeneratedFunction>,
)

/**
 * Enum model rendered by MineKot code generation helpers.
 *
 * @property packageName Generated package name.
 * @property name Generated enum name.
 * @property kdoc Generated enum documentation.
 * @property constants Enum constants.
 */
data class MineKotGeneratedEnum(
    val packageName: String,
    val name: String,
    val kdoc: String,
    val constants: List<String>,
)

/**
 * Sealed interface model rendered by MineKot code generation helpers.
 *
 * @property packageName Generated package name.
 * @property name Generated interface name.
 * @property kdoc Generated interface documentation.
 */
data class MineKotGeneratedSealedInterface(
    val packageName: String,
    val name: String,
    val kdoc: String,
)
