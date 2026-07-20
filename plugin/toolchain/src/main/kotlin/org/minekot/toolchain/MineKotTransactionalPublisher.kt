package org.minekot.toolchain

import org.gradle.api.GradleException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo

/** Publishes validated files with locking and complete rollback on failure. */
internal object MineKotTransactionalPublisher {
    /** Replaces all targets or restores every original byte sequence. */
    fun publish(
        root: Path,
        replacements: Map<Path, ByteArray>,
        transactionDirectory: Path,
        failureAfterMoves: Int? = null,
    ) {
        if (replacements.isEmpty()) {
            return
        }
        val normalizedRoot = root.toAbsolutePath().normalize()
        val normalizedReplacements = replacements.mapKeys { (path, _) -> path.toAbsolutePath().normalize() }
        require(normalizedReplacements.keys.all { path -> path.startsWith(normalizedRoot) }) {
            "MineKot transaction targets must remain under ${normalizedRoot}."
        }
        val transaction = transactionDirectory.resolve(UUID.randomUUID().toString()).createDirectories()
        val lockPath = transactionDirectory.resolve("publish.lock")
        lockPath.parent.createDirectories()
        FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            val lock = channel.tryLock() ?: throw GradleException("Another MineKot source transaction is active.")
            lock.use {
                publishLocked(normalizedRoot, normalizedReplacements, transaction, failureAfterMoves)
            }
        }
    }

    private fun publishLocked(
        root: Path,
        replacements: Map<Path, ByteArray>,
        transaction: Path,
        failureAfterMoves: Int?,
    ) {
        val originals = replacements.keys.associateWith { target ->
            target.takeIf(Path::isRegularFile)?.let(Files::readAllBytes)
        }
        val temporaryFiles = replacements.mapValues { (target, bytes) ->
            target.parent.createDirectories()
            val temporary = target.resolveSibling(".${target.fileName}.minekot-${UUID.randomUUID()}.tmp")
            FileChannel.open(
                temporary,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            ).use { channel ->
                channel.write(ByteBuffer.wrap(bytes))
                channel.force(true)
            }
            temporary
        }
        val moved = mutableListOf<Path>()
        runCatching {
            replacements.keys.sortedBy { path -> path.relativeTo(root).toString() }.forEachIndexed { index, target ->
                if (failureAfterMoves != null && index == failureAfterMoves) {
                    error("Injected MineKot publication failure after ${failureAfterMoves} moves.")
                }
                atomicReplace(temporaryFiles.getValue(target), target)
                moved.add(target)
            }
        }.onFailure { failure ->
            val rollbackFailures = moved.asReversed().mapNotNull { target ->
                runCatching {
                    val original = originals.getValue(target)
                    if (original == null) {
                        Files.deleteIfExists(target)
                    } else {
                        val restore =
                            target.resolveSibling(".${target.fileName}.minekot-restore-${UUID.randomUUID()}.tmp")
                        Files.write(restore, original, StandardOpenOption.CREATE_NEW)
                        atomicReplace(restore, target)
                    }
                }.exceptionOrNull()
            }
            temporaryFiles.values.forEach { temporary -> Files.deleteIfExists(temporary) }
            if (rollbackFailures.isNotEmpty()) {
                rollbackFailures.forEach(failure::addSuppressed)
            }
            throw GradleException("MineKot source transaction failed; original sources were restored.", failure)
        }
        transaction.toFile().deleteRecursively()
    }

    private fun atomicReplace(
        source: Path,
        target: Path,
    ) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (failure: AtomicMoveNotSupportedException) {
            throw GradleException("Atomic source replacement is not supported for ${target}.", failure)
        }
    }
}
