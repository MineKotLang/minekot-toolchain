package org.minekot.codegen.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import java.io.IOException

/**
 * Writes a KotlinPoet file through KSP with originating files.
 *
 * @param codeGenerator KSP code generator.
 * @param originatingFiles Source files used to generate the file.
 * @throws IOException If writing fails.
 */
@Throws(IOException::class)
fun FileSpec.writeMineKotTo(codeGenerator: CodeGenerator, vararg originatingFiles: KSFile) {
    codeGenerator.createNewFile(
        dependencies = Dependencies(aggregating = false, *originatingFiles),
        packageName = packageName,
        fileName = name,
    ).bufferedWriter().use { writer ->
        writer.write(toString())
    }
}
