package app.revanced.manager.patcher

import app.revanced.library.ApkUtils.applyTo
import app.revanced.manager.patcher.Session.Companion.component1
import app.revanced.manager.patcher.Session.Companion.component2
import app.revanced.manager.patcher.logger.Logger
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal typealias PatchList = List<Patch<*>>

class Session(
    cacheDir: String,
    frameworkDir: String,
    aaptPath: String,
    private val logger: Logger,
    private val input: File,
    private val onEvent: (ProgressEvent) -> Unit,
) : Closeable {
    private val tempDir = File(cacheDir).resolve("patcher").also { it.mkdirs() }
    private val patcher = Patcher(
        PatcherConfig(
            apkFile = input,
            temporaryFilesPath = tempDir,
            frameworkFileDirectory = frameworkDir,
            aaptBinaryPath = aaptPath
        )
    )

    private suspend fun Patcher.applyPatchesVerbose(selectedPatches: PatchList) {
        this().collect { (patch, exception) ->
            val index = selectedPatches.indexOf(patch)
            if (index == -1) return@collect

            if (exception != null) {
                onEvent(
                    ProgressEvent.Failed(
                        StepId.ExecutePatch(index),
                        exception.toRemoteError(),
                    )
                )
                logger.error("${patch.name} failed:")
                logger.error(exception.stackTraceToString())
                throw exception
            }

            onEvent(
                ProgressEvent.Completed(
                    StepId.ExecutePatch(index),
                )
            )

            logger.info("${patch.name} succeeded")
        }
    }

    suspend fun run(output: File, selectedPatches: PatchList) {
        runStep(StepId.ExecutePatches, onEvent) {
            java.util.logging.Logger.getLogger("").apply {
                handlers.forEach {
                    it.close()
                    removeHandler(it)
                }

                addHandler(logger.handler)
            }

            with(patcher) {
                logger.info("Merging integrations")
                this += selectedPatches.toSet()

                logger.info("Applying patches...")
                applyPatchesVerbose(selectedPatches.sortedBy { it.name })
            }
        }

        runStep(StepId.WriteAPK, onEvent) {
            logger.info("Writing patched files...")
            val result = patcher.get()

            val patched = tempDir.resolve("result.apk")
            withContext(Dispatchers.IO) {
                Files.copy(input.toPath(), patched.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            result.applyTo(patched)

            logger.info("Patched apk saved to $patched")

            withContext(Dispatchers.IO) {
                Files.move(patched.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    override fun close() {
        tempDir.deleteRecursively()
        patcher.close()
    }

    companion object {
        operator fun PatchResult.component1() = patch
        operator fun PatchResult.component2() = exception
    }
}