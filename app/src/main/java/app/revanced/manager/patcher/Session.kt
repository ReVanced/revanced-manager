package app.revanced.manager.patcher

import app.revanced.library.ApkUtils.applyTo
import app.revanced.manager.patcher.Session.Companion.component1
import app.revanced.manager.patcher.Session.Companion.component2
import app.revanced.manager.patcher.logger.Logger
import app.revanced.patcher.PatchesResult
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal typealias PatchList = List<Patch>
private typealias Patcher = (emit: (PatchResult) -> Unit) -> PatchesResult

class Session(
    cacheDir: String,
    private val frameworkDir: String,
    private val aaptPath: String,
    private val logger: Logger,
    private val input: File,
    private val onEvent: (ProgressEvent) -> Unit,
) : Closeable {
    private val tempDir = File(cacheDir).resolve("patcher").also { it.mkdirs() }

    private suspend fun applyPatchesVerbose(patcher: Patcher, selectedPatches: PatchList) =
        withContext(
            Dispatchers.Default
        ) {
            val context = currentCoroutineContext()
            patcher { (patch, exception) ->
                // Make the patching process cancelable.
                context.ensureActive()

                val index = selectedPatches.indexOf(patch)
                if (index == -1) return@patcher

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
        val result = runStep(StepId.ExecutePatches, onEvent) {
            java.util.logging.Logger.getLogger("").apply {
                handlers.forEach {
                    it.close()
                    removeHandler(it)
                }

                addHandler(logger.handler)
            }

            val patcher = patcher(
                apkFile = input,
                temporaryFilesPath = tempDir,
                frameworkFileDirectory = frameworkDir,
                aaptBinaryPath = File(aaptPath)
            ) { _packageName, _version ->
                selectedPatches.toSet()
            }

            logger.info("Applying patches...")
            applyPatchesVerbose(patcher, selectedPatches.sortedBy { it.name })
        }

        runStep(StepId.WriteAPK, onEvent) {
            logger.info("Writing patched files...")

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
        //patcher.close()
    }

    companion object {
        operator fun PatchResult.component1() = patch
        operator fun PatchResult.component2() = exception
    }
}